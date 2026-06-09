import { useEffect, useMemo, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AppError, ErrorCode } from "@/lib/errors";
import {
  buildInterviewReportViewModel,
  fetchInterviewReportQueryData,
  type ReportQueryData,
} from "@/hooks/interview/report/interviewReportData.shared";
import { interviewService } from "@/services/interviewService";

const REPORT_RECORDING_RETRY_DELAY_MS = 1500;
const REPORT_RECORDING_RETRY_ATTEMPTS = 40;
const REPORT_REFERENCE_ANSWER_RETRY_DELAY_MS = 2_000;
const REPORT_REFERENCE_ANSWER_RETRY_ATTEMPTS = 45;
const REPORT_AI_REVIEW_RETRY_DELAY_MS = 2_000;
const REPORT_AI_REVIEW_RETRY_ATTEMPTS = 45;

const waitForRetry = (delayMs: number) =>
  new Promise((resolve) => {
    window.setTimeout(resolve, delayMs);
  });

const hasMissingReferenceAnswers = (record: ReportQueryData["record"]) => {
  const qaReviews = buildInterviewReportViewModel(record).qaReviews;
  if (qaReviews.length === 0) {
    return true;
  }
  return qaReviews.some(
    (item) => !item.referenceAnswer || item.referenceAnswer.trim().length === 0,
  );
};

const isReferenceAnswerRetryableError = (error: unknown) =>
  error instanceof AppError && error.code === ErrorCode.REQUEST_TIMEOUT;

const hasAiReviewFeedback = (record: ReportQueryData["record"]) => {
  const feedback = buildInterviewReportViewModel(record).reviewFeedback;
  return feedback.source === "ai" && Boolean(feedback.overallComment);
};

const getReviewFeedbackSignature = (record: ReportQueryData["record"]) => {
  const feedback = buildInterviewReportViewModel(record).reviewFeedback;
  return JSON.stringify({
    overallComment: feedback.overallComment ?? null,
    highlights: feedback.highlights,
    improvementTips: feedback.improvementTips,
    nextActions: feedback.nextActions,
  });
};

const markRecordReviewFeedbackAsAi = (
  record: ReportQueryData["record"],
): ReportQueryData["record"] => {
  if (!record?.reviewFeedback) {
    return record;
  }
  return {
    ...record,
    reviewFeedback: {
      ...record.reviewFeedback,
      source: "ai",
    },
  };
};

export function useInterviewReportData(reportSessionId: string | null) {
  const queryClient = useQueryClient();
  const recordingRetryCountRef = useRef(0);

  const query = useQuery({
    queryKey: ["interview-record", reportSessionId],
    enabled: Boolean(reportSessionId),
    queryFn: () => fetchInterviewReportQueryData(reportSessionId as string),
    retry: false,
    refetchOnWindowFocus: false,
    staleTime: 60_000,
  });

  const recordError = useMemo(() => {
    if (!query.error) return null;
    return query.error instanceof Error
      ? query.error.message
      : "加载面试报告时发生错误，请稍后重试。";
  }, [query.error]);

  const generateReferenceAnswersMutation = useMutation({
    mutationFn: async () => {
      if (!reportSessionId) {
        throw new Error("missing report session id");
      }
      try {
        return await interviewService.generateInterviewReferenceAnswers(
          reportSessionId,
        );
      } catch (error) {
        if (!isReferenceAnswerRetryableError(error)) {
          throw error;
        }

        for (
          let attempt = 0;
          attempt < REPORT_REFERENCE_ANSWER_RETRY_ATTEMPTS;
          attempt += 1
        ) {
          await waitForRetry(REPORT_REFERENCE_ANSWER_RETRY_DELAY_MS);
          try {
            const record =
              await interviewService.getInterviewRecordBySessionId(
                reportSessionId,
              );
            if (record && !hasMissingReferenceAnswers(record)) {
              return record;
            }
          } catch (pollError) {
            if (!isReferenceAnswerRetryableError(pollError)) {
              throw pollError;
            }
          }
        }

        throw error;
      }
    },
    onSuccess: (record) => {
      if (!reportSessionId) {
        return;
      }
      queryClient.setQueryData<ReportQueryData>(
        ["interview-record", reportSessionId],
        { record },
      );
    },
  });

  const generateAiReviewFeedbackMutation = useMutation({
    mutationFn: async () => {
      if (!reportSessionId) {
        throw new Error("missing report session id");
      }
      const baselineRecord =
        queryClient.getQueryData<ReportQueryData>([
          "interview-record",
          reportSessionId,
        ])?.record ?? null;
      const baselineSignature = getReviewFeedbackSignature(baselineRecord);
      try {
        const record = await interviewService.generateInterviewAiReviewFeedback(
          reportSessionId,
        );
        return markRecordReviewFeedbackAsAi(record);
      } catch (error) {
        if (!(error instanceof AppError) || error.code !== ErrorCode.REQUEST_TIMEOUT) {
          throw error;
        }

        for (
          let attempt = 0;
          attempt < REPORT_AI_REVIEW_RETRY_ATTEMPTS;
          attempt += 1
        ) {
          await waitForRetry(REPORT_AI_REVIEW_RETRY_DELAY_MS);
          try {
            const record =
              await interviewService.getInterviewRecordBySessionId(
                reportSessionId,
              );
            const currentSignature = getReviewFeedbackSignature(record);
            if (
              record &&
              (hasAiReviewFeedback(record) ||
                currentSignature !== baselineSignature)
            ) {
              return markRecordReviewFeedbackAsAi(record);
            }
          } catch (pollError) {
            if (
              !(pollError instanceof AppError) ||
              pollError.code !== ErrorCode.REQUEST_TIMEOUT
            ) {
              throw pollError;
            }
          }
        }

        throw error;
      }
    },
    onSuccess: (record) => {
      if (!reportSessionId) {
        return;
      }
      queryClient.setQueryData<ReportQueryData>(
        ["interview-record", reportSessionId],
        { record },
      );
    },
  });

  const currentRecord =
    generateAiReviewFeedbackMutation.data ??
    generateReferenceAnswersMutation.data ??
    query.data?.record ??
    null;

  const reportViewModel = useMemo(
    () => buildInterviewReportViewModel(currentRecord),
    [currentRecord],
  );

  const canGenerateReferenceAnswers = useMemo(
    () =>
      Boolean(reportSessionId) &&
      reportViewModel.qaReviews.length > 0 &&
      reportViewModel.qaReviews.some(
        (item) =>
          !item.referenceAnswer || item.referenceAnswer.trim().length === 0,
      ),
    [reportSessionId, reportViewModel.qaReviews],
  );

  const canGenerateAiReviewFeedback = useMemo(
    () =>
      Boolean(reportSessionId) &&
      reportViewModel.qaReviews.length > 0 &&
      reportViewModel.reviewFeedback.source !== "ai",
    [reportSessionId, reportViewModel.qaReviews.length, reportViewModel.reviewFeedback.source],
  );

  useEffect(() => {
    recordingRetryCountRef.current = 0;
  }, [reportSessionId]);

  useEffect(() => {
    if (
      !reportSessionId ||
      query.isLoading ||
      query.isFetching ||
      !currentRecord ||
      generateReferenceAnswersMutation.isPending ||
      generateAiReviewFeedbackMutation.isPending
    ) {
      return;
    }
    if (reportViewModel.recordingUrl) {
      recordingRetryCountRef.current = 0;
      return;
    }
    if (recordingRetryCountRef.current >= REPORT_RECORDING_RETRY_ATTEMPTS) {
      return;
    }

    const timerId = window.setTimeout(() => {
      recordingRetryCountRef.current += 1;
      void query.refetch();
    }, REPORT_RECORDING_RETRY_DELAY_MS);

    return () => {
      window.clearTimeout(timerId);
    };
  }, [
    currentRecord,
    generateAiReviewFeedbackMutation.isPending,
    generateReferenceAnswersMutation.isPending,
    query,
    reportSessionId,
    reportViewModel.recordingUrl,
  ]);

  return {
    isRecordLoading: query.isLoading || query.isFetching,
    recordError,
    record: currentRecord,
    isGeneratingAiReviewFeedback: generateAiReviewFeedbackMutation.isPending,
    canGenerateAiReviewFeedback,
    isGeneratingReferenceAnswers: generateReferenceAnswersMutation.isPending,
    canGenerateReferenceAnswers,
    generateAiReviewFeedback: async () => {
      if (!reportSessionId) {
        return null;
      }
      return generateAiReviewFeedbackMutation.mutateAsync();
    },
    generateReferenceAnswers: async () => {
      if (!reportSessionId) {
        return null;
      }
      return generateReferenceAnswersMutation.mutateAsync();
    },
    ...reportViewModel,
  };
}

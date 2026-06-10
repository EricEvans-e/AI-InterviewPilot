import { act, renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppError, ErrorCode } from "@/lib/errors";
import { useInterviewReportData } from "@/hooks/interview/report/useInterviewReportData";
import { interviewService } from "@/services/interviewService";
import * as reportShared from "@/hooks/interview/report/interviewReportData.shared";

afterEach(() => {
  vi.restoreAllMocks();
});

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe("useInterviewReportData", () => {
  it("manually generates reference answers and updates the cached report", async () => {
    vi.spyOn(reportShared, "fetchInterviewReportQueryData").mockResolvedValue({
      record: {
        id: 1,
        userId: 1,
        sessionId: "session-manual-ref",
        playbackItems: [
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 70,
            feedback: "need more detail",
          },
        ],
      },
    });
    const generateSpy = vi
      .spyOn(interviewService, "generateInterviewReferenceAnswers")
      .mockResolvedValue({
        id: 1,
        userId: 1,
        sessionId: "session-manual-ref",
        playbackItems: [
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 70,
            feedback: "need more detail",
            referenceAnswer: "reference answer",
          },
        ],
      });

    const { result } = renderHook(
      () => useInterviewReportData("session-manual-ref"),
      { wrapper: createWrapper() },
    );

    await waitFor(() => {
      expect(result.current.isRecordLoading).toBe(false);
    });

    expect(result.current.canGenerateReferenceAnswers).toBe(true);
    expect(result.current.qaReviews[0]?.referenceAnswer).toBeUndefined();

    await act(async () => {
      await result.current.generateReferenceAnswers();
    });

    expect(generateSpy).toHaveBeenCalledWith("session-manual-ref");
    await waitFor(() => {
      expect(result.current.qaReviews[0]?.referenceAnswer).toBe(
        "reference answer",
      );
    });
    expect(result.current.canGenerateReferenceAnswers).toBe(false);
  });

  it("refetches the report when recording url is still pending", async () => {
    const fetchSpy = vi
      .spyOn(reportShared, "fetchInterviewReportQueryData")
      .mockResolvedValueOnce({
        record: {
          id: 2,
          userId: 1,
          sessionId: "session-recording-pending",
          playbackItems: [
            {
              questionNumber: "1",
              question: "Q1",
              answer: "A1",
              score: 70,
            },
          ],
        },
      })
      .mockResolvedValueOnce({
        record: {
          id: 2,
          userId: 1,
          sessionId: "session-recording-pending",
          recordingUrl: "/recordings/demo.webm",
          playbackItems: [
            {
              questionNumber: "1",
              question: "Q1",
              answer: "A1",
              score: 70,
            },
          ],
        },
      });

    const { result } = renderHook(
      () => useInterviewReportData("session-recording-pending"),
      { wrapper: createWrapper() },
    );

    await waitFor(() => {
      expect(result.current.isRecordLoading).toBe(false);
    });

    expect(result.current.recordingUrl).toBeNull();

    await waitFor(
      () => {
        expect(result.current.recordingUrl).toBe("/recordings/demo.webm");
      },
      { timeout: 3000 },
    );
    expect(fetchSpy).toHaveBeenCalledTimes(2);
  }, 5000);

  it("polls the report after reference-answer generation times out and adopts the generated answer", async () => {
    vi.spyOn(reportShared, "fetchInterviewReportQueryData").mockResolvedValue({
      record: {
        id: 3,
        userId: 1,
        sessionId: "session-ref-timeout",
        recordingUrl: "/recordings/demo.webm",
        playbackItems: [
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 70,
            feedback: "need more detail",
          },
        ],
      },
    });
    vi.spyOn(interviewService, "getInterviewRecordBySessionId").mockResolvedValueOnce({
      id: 3,
      userId: 1,
      sessionId: "session-ref-timeout",
      recordingUrl: "/recordings/demo.webm",
      playbackItems: [
        {
          questionNumber: "1",
          question: "Q1",
          answer: "A1",
          score: 70,
          feedback: "need more detail",
          referenceAnswer: "Generated answer",
        },
      ],
    });
    const generateSpy = vi
      .spyOn(interviewService, "generateInterviewReferenceAnswers")
      .mockRejectedValueOnce(
        new AppError(ErrorCode.REQUEST_TIMEOUT, "Request timeout"),
      );

    const { result } = renderHook(
      () => useInterviewReportData("session-ref-timeout"),
      { wrapper: createWrapper() },
    );

    await waitFor(() => {
      expect(result.current.isRecordLoading).toBe(false);
    });

    await act(async () => {
      await result.current.generateReferenceAnswers();
    });

    expect(generateSpy).toHaveBeenCalledWith("session-ref-timeout");
    await waitFor(() => {
      expect(result.current.qaReviews[0]?.referenceAnswer).toBe(
        "Generated answer",
      );
    });
  }, 10000);

  it("keeps polling after reference-answer generation succeeds but the returned report is still stale", async () => {
    vi.spyOn(reportShared, "fetchInterviewReportQueryData").mockResolvedValue({
      record: {
        id: 31,
        userId: 1,
        sessionId: "session-ref-stale-success",
        recordingUrl: "/recordings/demo.webm",
        playbackItems: [
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 70,
            feedback: "need more detail",
          },
        ],
      },
    });
    const getRecordSpy = vi
      .spyOn(interviewService, "getInterviewRecordBySessionId")
      .mockResolvedValueOnce({
        id: 31,
        userId: 1,
        sessionId: "session-ref-stale-success",
        recordingUrl: "/recordings/demo.webm",
        playbackItems: [
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 70,
            feedback: "need more detail",
            referenceAnswer: "Polled generated answer",
          },
        ],
      });
    vi.spyOn(
      interviewService,
      "generateInterviewReferenceAnswers",
    ).mockResolvedValue({
      id: 31,
      userId: 1,
      sessionId: "session-ref-stale-success",
      recordingUrl: "/recordings/demo.webm",
      playbackItems: [
        {
          questionNumber: "1",
          question: "Q1",
          answer: "A1",
          score: 70,
          feedback: "need more detail",
        },
      ],
    });

    const { result } = renderHook(
      () => useInterviewReportData("session-ref-stale-success"),
      { wrapper: createWrapper() },
    );

    await waitFor(() => {
      expect(result.current.isRecordLoading).toBe(false);
    });

    await act(async () => {
      await result.current.generateReferenceAnswers();
    });

    await waitFor(
      () => {
        expect(result.current.qaReviews[0]?.referenceAnswer).toBe(
          "Polled generated answer",
        );
      },
      { timeout: 5000 },
    );
    expect(getRecordSpy).toHaveBeenCalledTimes(1);
  }, 7000);

  it("shows incremental reference-answer progress even when some questions are still missing answers", async () => {
    vi.spyOn(reportShared, "fetchInterviewReportQueryData").mockResolvedValue({
      record: {
        id: 32,
        userId: 1,
        sessionId: "session-ref-partial-progress",
        recordingUrl: "/recordings/demo.webm",
        playbackItems: [
          {
            questionNumber: "1",
            question: "Main Q",
            answer: "A1",
            score: 70,
            feedback: "good",
            referenceAnswer: "preset main answer",
          },
          {
            questionNumber: "1-F1",
            question: "Follow up 1",
            answer: "A2",
            score: 60,
            feedback: "need more detail",
          },
          {
            questionNumber: "1-F2",
            question: "Follow up 2",
            answer: "A3",
            score: 55,
            feedback: "need more detail",
          },
        ],
      },
    });
    vi.spyOn(interviewService, "generateInterviewReferenceAnswers").mockRejectedValueOnce(
      new AppError(ErrorCode.REQUEST_TIMEOUT, "Request timeout"),
    );
    const getRecordSpy = vi
      .spyOn(interviewService, "getInterviewRecordBySessionId")
      .mockResolvedValueOnce({
        id: 32,
        userId: 1,
        sessionId: "session-ref-partial-progress",
        recordingUrl: "/recordings/demo.webm",
        playbackItems: [
          {
            questionNumber: "1",
            question: "Main Q",
            answer: "A1",
            score: 70,
            feedback: "good",
            referenceAnswer: "preset main answer",
          },
          {
            questionNumber: "1-F1",
            question: "Follow up 1",
            answer: "A2",
            score: 60,
            feedback: "need more detail",
            referenceAnswer: "generated follow up answer",
          },
          {
            questionNumber: "1-F2",
            question: "Follow up 2",
            answer: "A3",
            score: 55,
            feedback: "need more detail",
          },
        ],
      });

    const { result } = renderHook(
      () => useInterviewReportData("session-ref-partial-progress"),
      { wrapper: createWrapper() },
    );

    await waitFor(() => {
      expect(result.current.isRecordLoading).toBe(false);
    });

    await act(async () => {
      await result.current.generateReferenceAnswers();
    });

    await waitFor(
      () => {
        expect(result.current.qaReviews[1]?.referenceAnswer).toBe(
          "generated follow up answer",
        );
      },
      { timeout: 5000 },
    );
    expect(result.current.qaReviews[2]?.referenceAnswer).toBeUndefined();
    expect(result.current.canGenerateReferenceAnswers).toBe(true);
    expect(getRecordSpy).toHaveBeenCalledTimes(1);
  }, 7000);

  it("manually generates ai review feedback and updates the cached report", async () => {
    vi.spyOn(reportShared, "fetchInterviewReportQueryData").mockResolvedValue({
      record: {
        id: 4,
        userId: 1,
        sessionId: "session-manual-review",
        reviewFeedback: {
          overallComment: "rule summary",
          highlights: [],
          improvementTips: [],
          nextActions: [],
        },
        playbackItems: [
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 0,
            feedback: "need more detail",
          },
        ],
      },
    });
    const generateSpy = vi
      .spyOn(interviewService, "generateInterviewAiReviewFeedback")
      .mockResolvedValue({
        id: 4,
        userId: 1,
        sessionId: "session-manual-review",
        reviewFeedback: {
          overallComment: "AI summary",
          highlights: ["highlight 1"],
          improvementTips: ["improve 1"],
          nextActions: ["action 1"],
          source: "ai",
        },
        playbackItems: [
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 0,
            feedback: "need more detail",
          },
        ],
      });

    const { result } = renderHook(
      () => useInterviewReportData("session-manual-review"),
      { wrapper: createWrapper() },
    );

    await waitFor(() => {
      expect(result.current.isRecordLoading).toBe(false);
    });

    expect(result.current.reviewFeedback.overallComment).toBe("rule summary");
    expect(result.current.canGenerateAiReviewFeedback).toBe(true);

    await act(async () => {
      await result.current.generateAiReviewFeedback();
    });

    expect(generateSpy).toHaveBeenCalledWith("session-manual-review");
    await waitFor(() => {
      expect(result.current.reviewFeedback.overallComment).toBe("AI summary");
    });
    expect(result.current.reviewFeedback.highlights).toEqual(["highlight 1"]);
    expect(result.current.canGenerateAiReviewFeedback).toBe(false);
  });

  it("polls the report after ai review generation times out and adopts the generated feedback", async () => {
    vi.spyOn(reportShared, "fetchInterviewReportQueryData").mockResolvedValue({
      record: {
        id: 5,
        userId: 1,
        sessionId: "session-review-timeout",
        reviewFeedback: {
          overallComment: "rule summary",
          highlights: [],
          improvementTips: [],
          nextActions: [],
        },
        playbackItems: [
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 0,
            feedback: "need more detail",
          },
        ],
      },
    });
    vi.spyOn(interviewService, "getInterviewRecordBySessionId").mockResolvedValueOnce({
      id: 5,
      userId: 1,
      sessionId: "session-review-timeout",
      reviewFeedback: {
        overallComment: "AI summary after timeout",
        highlights: ["highlight 1"],
        improvementTips: ["improve 1"],
        nextActions: ["action 1"],
        source: "ai",
      },
      playbackItems: [
        {
          questionNumber: "1",
          question: "Q1",
          answer: "A1",
          score: 0,
          feedback: "need more detail",
        },
      ],
    });

    const generateSpy = vi
      .spyOn(interviewService, "generateInterviewAiReviewFeedback")
      .mockRejectedValueOnce(
        new AppError(ErrorCode.REQUEST_TIMEOUT, "Request timeout"),
      );

    const { result } = renderHook(
      () => useInterviewReportData("session-review-timeout"),
      { wrapper: createWrapper() },
    );

    await waitFor(() => {
      expect(result.current.isRecordLoading).toBe(false);
    });

    await act(async () => {
      await result.current.generateAiReviewFeedback();
    });

    expect(generateSpy).toHaveBeenCalledWith("session-review-timeout");
    await waitFor(() => {
      expect(result.current.reviewFeedback.overallComment).toBe(
        "AI summary after timeout",
      );
    });
  }, 10000);

  it("keeps polling after ai review generation succeeds but the returned report is still stale", async () => {
    vi.spyOn(reportShared, "fetchInterviewReportQueryData").mockResolvedValue({
      record: {
        id: 51,
        userId: 1,
        sessionId: "session-review-stale-success",
        reviewFeedback: {
          overallComment: "rule summary",
          highlights: [],
          improvementTips: [],
          nextActions: [],
        },
        playbackItems: [
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 0,
            feedback: "need more detail",
          },
        ],
      },
    });
    const getRecordSpy = vi
      .spyOn(interviewService, "getInterviewRecordBySessionId")
      .mockResolvedValueOnce({
        id: 51,
        userId: 1,
        sessionId: "session-review-stale-success",
        reviewFeedback: {
          source: "ai",
          overallComment: "Polled AI summary",
          highlights: ["highlight"],
          improvementTips: ["improve"],
          nextActions: ["action"],
        },
        playbackItems: [
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 0,
            feedback: "need more detail",
          },
        ],
      });
    vi.spyOn(
      interviewService,
      "generateInterviewAiReviewFeedback",
    ).mockResolvedValue({
      id: 51,
      userId: 1,
      sessionId: "session-review-stale-success",
      reviewFeedback: {
        overallComment: "rule summary",
        highlights: [],
        improvementTips: [],
        nextActions: [],
      },
      playbackItems: [
        {
          questionNumber: "1",
          question: "Q1",
          answer: "A1",
          score: 0,
          feedback: "need more detail",
        },
      ],
    });

    const { result } = renderHook(
      () => useInterviewReportData("session-review-stale-success"),
      { wrapper: createWrapper() },
    );

    await waitFor(() => {
      expect(result.current.isRecordLoading).toBe(false);
    });

    await act(async () => {
      await result.current.generateAiReviewFeedback();
    });

    await waitFor(
      () => {
        expect(result.current.reviewFeedback.overallComment).toBe(
          "Polled AI summary",
        );
      },
      { timeout: 5000 },
    );
    expect(getRecordSpy).toHaveBeenCalledTimes(1);
  }, 7000);
});

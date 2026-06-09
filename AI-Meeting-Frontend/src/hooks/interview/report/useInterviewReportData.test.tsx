import { act, renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppError, ErrorCode } from "@/lib/errors";
import { useInterviewReportData } from "@/hooks/interview/report/useInterviewReportData";
import { interviewService } from "@/services/interviewService";

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
    vi.spyOn(interviewService, "getInterviewRecordBySessionId").mockResolvedValue({
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
            referenceAnswer: "参考答案内容",
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
      expect(result.current.qaReviews[0]?.referenceAnswer).toBe("参考答案内容");
    });
    expect(result.current.canGenerateReferenceAnswers).toBe(false);
  });

  it("refetches the report when recording url is still pending", async () => {
    const getRecordSpy = vi
      .spyOn(interviewService, "getInterviewRecordBySessionId")
      .mockResolvedValueOnce({
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
      })
      .mockResolvedValueOnce({
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
    expect(getRecordSpy).toHaveBeenCalledTimes(2);
  }, 5000);

  it("polls the report after reference-answer generation times out and adopts the generated answer", async () => {
    vi.spyOn(interviewService, "getInterviewRecordBySessionId")
      .mockResolvedValueOnce({
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
      })
      .mockResolvedValueOnce({
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

  it("manually generates ai review feedback and updates the cached report", async () => {
    vi.spyOn(interviewService, "getInterviewRecordBySessionId").mockResolvedValue({
      id: 4,
      userId: 1,
      sessionId: "session-manual-review",
      reviewFeedback: {
        overallComment: "规则摘要",
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
    const generateSpy = vi
      .spyOn(interviewService, "generateInterviewAiReviewFeedback")
      .mockResolvedValue({
        id: 4,
        userId: 1,
        sessionId: "session-manual-review",
        reviewFeedback: {
          overallComment: "AI 总结",
          highlights: ["亮点 1"],
          improvementTips: ["改进 1"],
          nextActions: ["行动 1"],
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

    expect(result.current.reviewFeedback.overallComment).toBe("规则摘要");
    expect(result.current.canGenerateAiReviewFeedback).toBe(true);

    await act(async () => {
      await result.current.generateAiReviewFeedback();
    });

    expect(generateSpy).toHaveBeenCalledWith("session-manual-review");
    await waitFor(() => {
      expect(result.current.reviewFeedback.overallComment).toBe("AI 总结");
    });
    expect(result.current.reviewFeedback.highlights).toEqual(["亮点 1"]);
    expect(result.current.canGenerateAiReviewFeedback).toBe(false);
  });

  it("polls the report after ai review generation times out and adopts the generated feedback", async () => {
    vi.spyOn(interviewService, "getInterviewRecordBySessionId")
      .mockResolvedValueOnce({
        id: 5,
        userId: 1,
        sessionId: "session-review-timeout",
        reviewFeedback: {
          overallComment: "规则摘要",
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
      })
      .mockResolvedValueOnce({
        id: 5,
        userId: 1,
        sessionId: "session-review-timeout",
        reviewFeedback: {
          overallComment: "AI 超时后补齐的总结",
          highlights: ["亮点 1"],
          improvementTips: ["改进 1"],
          nextActions: ["行动 1"],
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
        "AI 超时后补齐的总结",
      );
    });
  }, 10000);
});

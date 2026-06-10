import { afterEach, describe, expect, it, vi } from "vitest";
import { AppError, ErrorCode } from "@/lib/errors";
import requestService from "@/lib/request";
import {
  type AnswerInterviewQuestionResult,
  interviewService,
  normalizeInterviewAnswer,
} from "@/services/interviewService";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("normalizeInterviewAnswer", () => {
  it("keeps isFollowUp and followUpNeeded independent", () => {
    const payload = {
      is_follow_up: false,
      follow_up_needed: true,
      next_question: "请说明缓存一致性方案",
      next_question_number: "1-F1",
      follow_up_count: "1",
      finished: false,
      isSuccess: true,
    } as unknown as AnswerInterviewQuestionResult;
    const normalized = normalizeInterviewAnswer(payload);

    expect(normalized.isFollowUp).toBe(false);
    expect(normalized.followUpNeeded).toBe(true);
    expect(normalized.nextQuestionNumber).toBe("1-F1");
    expect(normalized.followUpCount).toBe(1);
  });

  it("normalizes follow-up flags and score fields from mixed naming", () => {
    const payload = {
      isFollowUp: true,
      followUpNeeded: false,
      total_score: "88",
      score_comment: "回答结构清晰",
      next_question: "继续展开事务隔离级别的选择依据",
      next_question_number: "2-F2",
      follow_up_count: 2,
      finished: "false",
    } as unknown as AnswerInterviewQuestionResult;
    const normalized = normalizeInterviewAnswer(payload);

    expect(normalized.isFollowUp).toBe(true);
    expect(normalized.followUpNeeded).toBe(false);
    expect(normalized.totalScore).toBe(88);
    expect(normalized.feedback).toBe("回答结构清晰");
    expect(normalized.nextQuestionNumber).toBe("2-F2");
    expect(normalized.followUpCount).toBe(2);
    expect(normalized.finished).toBe(false);
  });
});

describe("interviewService.answerInterviewQuestion", () => {
  it("rejects empty questionNumber before request", async () => {
    const error = await interviewService
      .answerInterviewQuestion({
        sessionId: "session-1",
        questionNumber: "   ",
        answerContent: "answer",
      })
      .catch((caught) => caught);

    expect(error).toBeInstanceOf(AppError);
    expect((error as AppError).code).toBe(ErrorCode.CLIENT_VALIDATION_ERROR);
  });
});

describe("interviewService.saveInterviewRecordFromRedis", () => {
  it("does not fallback to legacy path for business operation failures", async () => {
    const postSpy = vi
      .spyOn(requestService, "post")
      .mockRejectedValueOnce(
        new AppError(
          ErrorCode.OPERATION_FAILED,
          "finalize is processing, please retry",
        ),
      )
      .mockResolvedValueOnce(undefined);

    const error = await interviewService
      .saveInterviewRecordFromRedis("session-1")
      .catch((caught) => caught);

    expect(error).toBeInstanceOf(AppError);
    expect((error as AppError).code).toBe(ErrorCode.OPERATION_FAILED);
    expect(postSpy).toHaveBeenCalledTimes(1);
    expect(postSpy).toHaveBeenCalledWith(
      "/ip/v1/interview/interview/record/save-from-redis/session-1",
      undefined,
      expect.objectContaining({ timeout: 60_000 }),
    );

    postSpy.mockRestore();
  });

  it("uses the extended report timeout when finalizing from redis", async () => {
    const postSpy = vi
      .spyOn(requestService, "post")
      .mockResolvedValueOnce(undefined);

    await interviewService.saveInterviewRecordFromRedis("session-timeout");

    expect(postSpy).toHaveBeenCalledWith(
      "/ip/v1/interview/interview/record/save-from-redis/session-timeout",
      undefined,
      expect.objectContaining({ timeout: 60_000 }),
    );
  });
});

describe("interviewService.generateInterviewReferenceAnswers", () => {
  it("falls back to legacy path when the new route is unavailable", async () => {
    const record = {
      id: 1,
      userId: 1,
      sessionId: "session-1",
    };

    const postSpy = vi
      .spyOn(requestService, "post")
      .mockRejectedValueOnce(
        new AppError(ErrorCode.RESOURCE_NOT_FOUND, "not found"),
      )
      .mockResolvedValueOnce(record);

    const result =
      await interviewService.generateInterviewReferenceAnswers("session-1");

    expect(result).toEqual(record);
    expect(postSpy).toHaveBeenNthCalledWith(
      1,
      "/ip/v1/interview/interview/record/session-1/reference-answers",
      undefined,
      expect.objectContaining({ timeout: 300_000 }),
    );
    expect(postSpy).toHaveBeenNthCalledWith(
      2,
      "/ip/v1/interview/record/session-1/reference-answers",
      undefined,
      expect.objectContaining({ timeout: 300_000 }),
    );

    postSpy.mockRestore();
  });

  it("uses the long ai timeout for manual reference-answer generation", async () => {
    const record = {
      id: 2,
      userId: 1,
      sessionId: "session-2",
    };
    const postSpy = vi
      .spyOn(requestService, "post")
      .mockResolvedValueOnce(record);

    await interviewService.generateInterviewReferenceAnswers("session-2");

    expect(postSpy).toHaveBeenCalledWith(
      "/ip/v1/interview/interview/record/session-2/reference-answers",
      undefined,
      expect.objectContaining({ timeout: 300_000 }),
    );
  });
});

describe("interviewService.generateInterviewAiReviewFeedback", () => {
  it("falls back to legacy path when the new route is unavailable", async () => {
    const record = {
      id: 11,
      userId: 1,
      sessionId: "session-review-1",
      reviewFeedback: {
        overallComment: "AI 总结",
      },
    };

    const postSpy = vi
      .spyOn(requestService, "post")
      .mockRejectedValueOnce(
        new AppError(ErrorCode.RESOURCE_NOT_FOUND, "not found"),
      )
      .mockResolvedValueOnce(record);

    const result =
      await interviewService.generateInterviewAiReviewFeedback(
        "session-review-1",
      );

    expect(result).toEqual(record);
    expect(postSpy).toHaveBeenNthCalledWith(
      1,
      "/ip/v1/interview/interview/record/session-review-1/review-feedback",
      undefined,
      expect.objectContaining({ timeout: 300_000 }),
    );
    expect(postSpy).toHaveBeenNthCalledWith(
      2,
      "/ip/v1/interview/record/session-review-1/review-feedback",
      undefined,
      expect.objectContaining({ timeout: 300_000 }),
    );
  });

  it("uses the long ai timeout for manual ai review generation", async () => {
    const record = {
      id: 12,
      userId: 1,
      sessionId: "session-review-2",
      reviewFeedback: {
        overallComment: "AI 总结",
      },
    };
    const postSpy = vi
      .spyOn(requestService, "post")
      .mockResolvedValueOnce(record);

    await interviewService.generateInterviewAiReviewFeedback("session-review-2");

    expect(postSpy).toHaveBeenCalledWith(
      "/ip/v1/interview/interview/record/session-review-2/review-feedback",
      undefined,
      expect.objectContaining({ timeout: 300_000 }),
    );
  });
});

describe("interviewService.getInterviewRecordBySessionId", () => {
  it("uses the extended report timeout while loading the report", async () => {
    const record = {
      id: 3,
      userId: 1,
      sessionId: "session-3",
    };
    const getSpy = vi.spyOn(requestService, "get").mockResolvedValueOnce(record);

    await interviewService.getInterviewRecordBySessionId("session-3");

    expect(getSpy).toHaveBeenCalledWith(
      "/ip/v1/interview/interview/record/session-3",
      expect.objectContaining({ timeout: 60_000 }),
    );
  });
});

import { afterEach, describe, expect, it, vi } from "vitest";
import requestService from "@/lib/request";
import { questionBankService } from "@/services/questionBankService";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("questionBankService.getQuestionCoverage", () => {
  it("loads question-bank coverage from the public question endpoint", async () => {
    const getSpy = vi.spyOn(requestService, "get").mockResolvedValueOnce({
      collegeId: 11,
      majorId: 22,
      interviewMode: "专业认知",
      approvedCount: 3,
      exactMatchCount: 1,
      fallbackCount: 2,
      canStartImmediately: true,
      mayNeedAiGeneration: true,
    });

    const result = await questionBankService.getQuestionCoverage({
      collegeId: 11,
      majorId: 22,
      interviewMode: "专业认知",
      requiredCount: 5,
      difficulty: "hard",
      abilityTag: "communication",
    });

    expect(result.approvedCount).toBe(3);
    expect(getSpy).toHaveBeenCalledWith("/ip/v1/questions/coverage", {
      params: {
        collegeId: 11,
        majorId: 22,
        interviewMode: "专业认知",
        requiredCount: 5,
        difficulty: "hard",
        abilityTag: "communication",
      },
    });
  });
});

import { describe, expect, it } from "vitest";
import { normalizeInterviewQuestionText } from "@/hooks/interview/session/interviewQuestionFormatting";

describe("normalizeInterviewQuestionText", () => {
  it("removes map-style question wrappers from model output", () => {
    const raw =
      "{question=在‘智控天眼’文本风控系统中，你提到了结合‘TF-IDF’与‘语义匹配’进行宣传语与评论的语义一致性校验。请具体描述一下？}";

    expect(normalizeInterviewQuestionText(raw)).toBe(
      "在‘智控天眼’文本风控系统中，你提到了结合‘TF-IDF’与‘语义匹配’进行宣传语与评论的语义一致性校验。请具体描述一下？",
    );
  });

  it("keeps normal question text unchanged apart from trimming", () => {
    expect(normalizeInterviewQuestionText("  请介绍一下你的项目。  ")).toBe(
      "请介绍一下你的项目。",
    );
  });
});

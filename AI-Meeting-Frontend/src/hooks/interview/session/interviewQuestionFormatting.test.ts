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

  it("extracts question text from richer java-map style payloads", () => {
    const raw =
      "{id=1, topic=NLP工程实践, question=在你的智控天眼风控系统中，你提到了使用RoBERTa-wwm-ext模型进行领域继续预训练和监督微调，并获得了95.23%的F1值。能否详细描述一下你针对消费场景风控这一特定领域，是如何设计领域继续预训练的数据和任务的？例如，你从哪里获取了高质量的无标注数据？预训练的掩码策略或任务设计与通用预训练有何不同？, purpose=考察候选人对NLP模型适配特定领域的实际工程理解深度，而不仅仅是调用API。问题直击其简历核心项目的技术细节。}";

    expect(normalizeInterviewQuestionText(raw)).toBe(
      "在你的智控天眼风控系统中，你提到了使用RoBERTa-wwm-ext模型进行领域继续预训练和监督微调，并获得了95.23%的F1值。能否详细描述一下你针对消费场景风控这一特定领域，是如何设计领域继续预训练的数据和任务的？例如，你从哪里获取了高质量的无标注数据？预训练的掩码策略或任务设计与通用预训练有何不同？",
    );
  });

  it("keeps normal question text unchanged apart from trimming", () => {
    expect(normalizeInterviewQuestionText("  请介绍一下你的项目。 ")).toBe(
      "请介绍一下你的项目。",
    );
  });
});

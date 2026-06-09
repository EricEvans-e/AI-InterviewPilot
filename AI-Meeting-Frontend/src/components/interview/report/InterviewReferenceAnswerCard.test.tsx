import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import InterviewReferenceAnswerCard from "@/components/interview/report/InterviewReferenceAnswerCard";

describe("InterviewReferenceAnswerCard", () => {
  it("does not show evaluation feedback as a reference answer", () => {
    render(
      <InterviewReferenceAnswerCard
        isLoading={false}
        qaReviews={[
          {
            questionNumber: "1",
            question: "请说明文本风控系统的语义一致性校验方案。",
            answer: "使用 TF-IDF 和语义匹配。",
            score: 66,
            feedback: "回答概述了方法框架，但缺乏实现细节。",
          },
        ]}
      />,
    );

    expect(screen.getByText("暂无参考答案")).toBeTruthy();
    expect(
      screen.queryByText("回答概述了方法框架，但缺乏实现细节。"),
    ).toBeNull();
    expect(
      screen.queryByRole("button", { name: /查看参考答案/u }),
    ).toBeNull();
  });

  it("shows generated or preset reference answers when present", () => {
    render(
      <InterviewReferenceAnswerCard
        isLoading={false}
        qaReviews={[
          {
            questionNumber: "1",
            question: "请说明文本风控系统的语义一致性校验方案。",
            answer: "使用 TF-IDF 和语义匹配。",
            score: 82,
            feedback: "回答结构清晰。",
            referenceAnswer:
              "参考回答应先说明 TF-IDF 用于快速召回关键词相似评论，再说明语义模型用于判断隐含表达一致性。",
          },
        ]}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /查看参考答案/u }));

    expect(
      screen.getByText(
        "参考回答应先说明 TF-IDF 用于快速召回关键词相似评论，再说明语义模型用于判断隐含表达一致性。",
      ),
    ).toBeTruthy();
    expect(screen.queryByText("回答结构清晰。")).toBeNull();
  });
});

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import InterviewReferenceAnswerCard from "@/components/interview/report/InterviewReferenceAnswerCard";

describe("InterviewReferenceAnswerCard", () => {
  it("shows manual generation action when no reference answers exist", () => {
    render(
      <InterviewReferenceAnswerCard
        isLoading={false}
        canGenerate={true}
        onGenerate={() => undefined}
        qaReviews={[
          {
            questionNumber: "1",
            question: "请说明文本风控系统的语义一致性校验方案。",
            answer: "使用 TF-IDF 和语义匹配。",
            score: 66,
            feedback: "回答概述了方法，但缺少实现细节。",
          },
        ]}
      />,
    );

    expect(screen.getByText("暂无参考答案")).toBeTruthy();
    expect(
      screen.queryByText("回答概述了方法，但缺少实现细节。"),
    ).toBeNull();
    expect(
      screen.getByRole("button", { name: "生成参考答案" }),
    ).toBeTruthy();
  });

  it("shows generated or preset reference answers when present", () => {
    render(
      <InterviewReferenceAnswerCard
        isLoading={false}
        canGenerate={false}
        qaReviews={[
          {
            questionNumber: "1",
            question: "请说明文本风控系统的语义一致性校验方案。",
            answer: "使用 TF-IDF 和语义匹配。",
            score: 82,
            feedback: "回答结构清晰。",
            referenceAnswer:
              "参考答案应先说明 TF-IDF 用于关键词召回与稀疏相似度计算，再说明语义模型用于判断隐含表达的一致性。",
          },
        ]}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /查看参考答案/u }));

    expect(
      screen.getByText(
        "参考答案应先说明 TF-IDF 用于关键词召回与稀疏相似度计算，再说明语义模型用于判断隐含表达的一致性。",
      ),
    ).toBeTruthy();
    expect(screen.queryByText("回答结构清晰。")).toBeNull();
  });

  it("fires manual generation callback when clicked", () => {
    const onGenerate = vi.fn();

    render(
      <InterviewReferenceAnswerCard
        isLoading={false}
        canGenerate={true}
        onGenerate={onGenerate}
        qaReviews={[
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 70,
            feedback: "need more detail",
          },
        ]}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "生成参考答案" }));

    expect(onGenerate).toHaveBeenCalledTimes(1);
  });

  it("shows generating state while the request is running", () => {
    render(
      <InterviewReferenceAnswerCard
        isLoading={false}
        canGenerate={true}
        isGenerating={true}
        onGenerate={() => undefined}
        qaReviews={[
          {
            questionNumber: "1",
            question: "Q1",
            answer: "A1",
            score: 70,
          },
        ]}
      />,
    );

    expect(
      screen.getByRole("button", { name: "生成中..." }),
    ).toHaveProperty("disabled", true);
  });
});

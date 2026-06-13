import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import InterviewHeader from "@/components/interview/InterviewHeader";

describe("InterviewHeader", () => {
  it("uses 面试间 as the room title", () => {
    render(
      <InterviewHeader
        isReady
        currentQuestionNumber="1"
        currentQuestionContent="请先做一个简短的自我介绍"
        isCurrentQuestionFollowUp={false}
        currentFollowUpCount={0}
        isInterviewFinished={false}
        totalInterviewScore={null}
        isCameraOpen={false}
        isEndingInterview={false}
        onToggleCamera={vi.fn()}
        onOpenSketchpad={vi.fn()}
        onEndInterview={vi.fn()}
      />,
    );

    expect(screen.getByText("面试间")).toBeTruthy();
  });

  it("shows self-introduction as the current stage for the first opening question", () => {
    render(
      <InterviewHeader
        isReady
        currentQuestionNumber="1"
        currentQuestionContent="请先做一个简短的自我介绍，重点介绍你的教育背景、相关经历，以及你与应聘方向的匹配度。"
        isCurrentQuestionFollowUp={false}
        currentFollowUpCount={0}
        isInterviewFinished={false}
        totalInterviewScore={null}
        isCameraOpen={false}
        isEndingInterview={false}
        onToggleCamera={vi.fn()}
        onOpenSketchpad={vi.fn()}
        onEndInterview={vi.fn()}
      />,
    );

    expect(screen.getByText("当前环节：自我介绍")).toBeTruthy();
  });

  it("keeps the numbered label for regular questions", () => {
    render(
      <InterviewHeader
        isReady
        currentQuestionNumber="2"
        currentQuestionContent="请介绍一下你做过的项目。"
        isCurrentQuestionFollowUp={false}
        currentFollowUpCount={0}
        isInterviewFinished={false}
        totalInterviewScore={null}
        isCameraOpen={false}
        isEndingInterview={false}
        onToggleCamera={vi.fn()}
        onOpenSketchpad={vi.fn()}
        onEndInterview={vi.fn()}
      />,
    );

    expect(screen.getByText("当前题号：2")).toBeTruthy();
  });
});

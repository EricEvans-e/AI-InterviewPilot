import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import InterviewQaReplayCard from "@/components/interview/report/InterviewQaReplayCard";

describe("InterviewQaReplayCard", () => {
  it("labels the first self introduction main question explicitly", () => {
    render(
      <MemoryRouter>
        <InterviewQaReplayCard
          qaReviews={[
            {
              questionNumber: "1",
              question: "请先做一个自我介绍",
              answer: "我是张三，来自软件工程专业。",
              score: 90,
              feedback: "结构完整。",
            },
          ]}
          isRecordLoading={false}
          recordError={null}
        />
      </MemoryRouter>,
    );

    expect(screen.getByText("自我介绍")).toBeTruthy();
    expect(screen.queryByText("主问题 1")).toBeNull();
  });

  it("keeps follow up labels unchanged", () => {
    render(
      <MemoryRouter>
        <InterviewQaReplayCard
          qaReviews={[
            {
              questionNumber: "1",
              question: "请先做一个自我介绍",
              answer: "我是张三，来自软件工程专业。",
              score: 90,
              feedback: "结构完整。",
            },
            {
              questionNumber: "1-F1",
              question: "你最有代表性的项目是什么？",
              answer: "校园 AI 助手项目。",
              score: 85,
              feedback: "项目说明还可以更量化。",
              isFollowUp: true,
              followUpCount: 1,
            },
          ]}
          isRecordLoading={false}
          recordError={null}
        />
      </MemoryRouter>,
    );

    expect(screen.getByText("自我介绍")).toBeTruthy();
    expect(screen.getByText("追问 1-F1（第 1 轮）")).toBeTruthy();
  });
});

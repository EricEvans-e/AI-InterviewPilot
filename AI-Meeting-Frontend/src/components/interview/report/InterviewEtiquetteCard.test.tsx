import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import InterviewEtiquetteCard from "@/components/interview/report/InterviewEtiquetteCard";
import type { InterviewRecordResult } from "@/services/interviewService";

describe("InterviewEtiquetteCard", () => {
  it("shows demeanor performance and etiquette posture as separate 100-point scores", () => {
    const record = {
      id: 1,
      userId: 1,
      sessionId: "session-1",
      radarChart: {
        etiquetteScore: 76,
        demeanorEvaluation: 0,
      },
      sessionSnapshotJson: JSON.stringify({
        demeanorScore: 0,
        demeanorDetails: {
          panicLevel: 20,
          seriousnessLevel: 80,
          emoticonHandling: 75,
          compositeScore: 82,
        },
      }),
    } satisfies InterviewRecordResult;

    render(<InterviewEtiquetteCard record={record} isLoading={false} />);

    expect(screen.getByText("礼仪仪态得分")).toBeTruthy();
    expect(screen.getByText("神态表现得分")).toBeTruthy();
    expect(screen.getByText("76")).toBeTruthy();
    expect(screen.getAllByText("82").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("/ 100")).toHaveLength(2);
    expect(screen.queryByText("神态综合得分")).toBeNull();
  });

  it("shows evaluation basis and improvement suggestions from AI payload when available", () => {
    const record = {
      id: 1,
      userId: 1,
      sessionId: "session-1",
      radarChart: {
        etiquetteScore: 68,
        demeanorEvaluation: 72,
      },
      sessionSnapshotJson: JSON.stringify({
        demeanorDetails: {
          panicLevel: 62,
          seriousnessLevel: 72,
          emoticonHandling: 60,
          compositeScore: 72,
          evaluationBasis: [
            "AI 依据摄像头截图中的坐姿、表情稳定性和专注状态进行综合判断。",
          ],
          improvementSuggestions: [
            "回答前先停顿一秒并保持正视镜头，减少频繁低头和表情紧绷。",
          ],
        },
      }),
    } satisfies InterviewRecordResult;

    render(<InterviewEtiquetteCard record={record} isLoading={false} />);

    expect(screen.getByText("评估依据")).toBeTruthy();
    expect(screen.getByText("改进建议")).toBeTruthy();
    expect(
      screen.getByText(
        "AI 依据摄像头截图中的坐姿、表情稳定性和专注状态进行综合判断。",
      ),
    ).toBeTruthy();
    expect(
      screen.getByText(
        "回答前先停顿一秒并保持正视镜头，减少频繁低头和表情紧绷。",
      ),
    ).toBeTruthy();
  });

  it("falls back to score-based basis and suggestions when AI text is absent", () => {
    const record = {
      id: 1,
      userId: 1,
      sessionId: "session-1",
      radarChart: {
        etiquetteScore: 58,
      },
      sessionSnapshotJson: JSON.stringify({
        demeanorDetails: {
          panicLevel: 70,
          seriousnessLevel: 55,
          emoticonHandling: 45,
          compositeScore: 50,
        },
      }),
    } satisfies InterviewRecordResult;

    render(<InterviewEtiquetteCard record={record} isLoading={false} />);

    expect(screen.getByText("评估依据")).toBeTruthy();
    expect(screen.getByText("改进建议")).toBeTruthy();
    expect(screen.getByText(/礼仪仪态得分为 58\/100/)).toBeTruthy();
    expect(screen.getByText(/紧张或慌乱迹象较明显/)).toBeTruthy();
    expect(screen.getByText(/回答前先稳定呼吸/)).toBeTruthy();
  });
});

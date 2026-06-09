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
});

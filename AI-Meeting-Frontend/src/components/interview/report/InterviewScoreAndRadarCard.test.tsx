import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import InterviewScoreAndRadarCard from "@/components/interview/report/InterviewScoreAndRadarCard";

describe("InterviewScoreAndRadarCard", () => {
  it("shows every dimension score on a 100-point scale", () => {
    render(
      <InterviewScoreAndRadarCard
        resumeScore={92}
        interviewScore={10}
        compositeScore={51}
        isCompositeEstimated
        radarPoints={[]}
        dimensionScores={{
          contentScore: 10,
          logicScore: 9,
          professionalScore: 8,
          expressionScore: 9,
          adaptabilityScore: 8,
          timeControlScore: 70,
          etiquetteScore: 70,
        }}
      />,
    );

    expect(screen.getByText("时间控制")).toBeTruthy();
    expect(screen.getByText("礼仪仪态")).toBeTruthy();
    expect(screen.getAllByText("/ 100")).toHaveLength(7);
    expect(screen.queryByText("/ 5")).toBeNull();
    expect(screen.queryByText("/ 10")).toBeNull();
  });
});

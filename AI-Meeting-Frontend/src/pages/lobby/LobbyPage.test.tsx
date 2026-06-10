import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import LobbyPage from "@/pages/lobby/LobbyPage";
import { useLobbyData } from "@/hooks/lobby/useLobbyData";

vi.mock("react-router-dom", () => ({
  useNavigate: () => vi.fn(),
}));

vi.mock("@/hooks/lobby/useLobbyData", () => ({
  useLobbyData: vi.fn(),
}));

vi.mock("@/components/lobby/LobbyFilterBar", () => ({
  default: () => <div data-testid="lobby-filter-bar" />,
}));

vi.mock("@/components/lobby/LobbyGrid", () => ({
  default: () => <div data-testid="lobby-grid" />,
}));

describe("LobbyPage", () => {
  it("shows only two start modes with explicit guidance", () => {
    vi.mocked(useLobbyData).mockReturnValue({
      filters: {
        questionTypes: [],
        abilityTags: [],
        difficulties: [],
      },
      colleges: [],
      collegesLoading: false,
      majors: [],
      majorsLoading: false,
      questions: [],
      totalQuestions: 0,
      totalPages: 0,
      questionsLoading: false,
      coverage: undefined,
      coverageInterviewMode: "综合题",
      coverageLoading: false,
      coverageError: null,
      pageNum: 1,
      setPageNum: vi.fn(),
      updateFilters: vi.fn(),
      resetFilters: vi.fn(),
      toggleArrayFilter: vi.fn(),
    });

    render(<LobbyPage />);

    expect(screen.getByText("开始练习")).toBeTruthy();
    expect(screen.getByText("全真模拟")).toBeTruthy();
    expect(screen.getByText("练习方式说明")).toBeTruthy();
    expect(
      screen.getByText("开始练习会基于当前筛选条件抽取 5 道题，适合日常练习。"),
    ).toBeTruthy();
    expect(
      screen.getByText("全真模拟会基于当前筛选条件抽取 10 道题，适合完整模拟。"),
    ).toBeTruthy();

    expect(screen.queryByText("随机模拟")).toBeNull();
    expect(screen.queryByText("按院校备考")).toBeNull();
    expect(screen.queryByText("按专业备考")).toBeNull();
    expect(screen.queryByText("按题型练习")).toBeNull();
  });
});

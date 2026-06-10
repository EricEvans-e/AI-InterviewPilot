import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { useLobbyData } from "@/hooks/lobby/useLobbyData";
import { questionBankService } from "@/services/questionBankService";

vi.mock("@/services/questionBankService", async () => {
  const actual = await vi.importActual<typeof import("@/services/questionBankService")>(
    "@/services/questionBankService",
  );
  return {
    ...actual,
    questionBankService: {
      pageQuestions: vi.fn(),
      listColleges: vi.fn(),
      listMajors: vi.fn(),
      getQuestionCoverage: vi.fn(),
    },
  };
});

afterEach(() => {
  vi.restoreAllMocks();
});

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe("useLobbyData", () => {
  it("loads coverage for the selected interview mode and target filters", async () => {
    vi.mocked(questionBankService.listColleges).mockResolvedValue([]);
    vi.mocked(questionBankService.listMajors).mockResolvedValue([]);
    vi.mocked(questionBankService.pageQuestions).mockResolvedValue({
      records: [],
      total: 0,
      size: 20,
      current: 1,
      pages: 0,
    });
    vi.mocked(questionBankService.getQuestionCoverage).mockResolvedValue({
      collegeId: 11,
      majorId: 22,
      interviewMode: "专业题",
      approvedCount: 4,
      exactMatchCount: 2,
      fallbackCount: 2,
      canStartImmediately: true,
      mayNeedAiGeneration: true,
    });

    const { result } = renderHook(() => useLobbyData("专业题", 5), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.updateFilters({
        collegeId: 11,
        majorId: 22,
      });
    });

    await waitFor(() =>
      expect(questionBankService.getQuestionCoverage).toHaveBeenLastCalledWith({
        collegeId: 11,
        majorId: 22,
        interviewMode: "专业题",
        requiredCount: 5,
      }),
    );
    await waitFor(() => expect(result.current.coverage?.approvedCount).toBe(4));
  });

  it("syncs single-select question type, difficulty and ability tag into question and coverage queries", async () => {
    vi.mocked(questionBankService.listColleges).mockResolvedValue([]);
    vi.mocked(questionBankService.listMajors).mockResolvedValue([]);
    vi.mocked(questionBankService.pageQuestions).mockResolvedValue({
      records: [],
      total: 0,
      size: 20,
      current: 1,
      pages: 0,
    });
    vi.mocked(questionBankService.getQuestionCoverage).mockResolvedValue({
      approvedCount: 2,
      exactMatchCount: 1,
      fallbackCount: 1,
      canStartImmediately: true,
      mayNeedAiGeneration: true,
    });

    const { result } = renderHook(() => useLobbyData("综合题", 4), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.updateFilters({ collegeId: 9, majorId: 18 });
      result.current.toggleArrayFilter("questionTypes", "专业题");
      result.current.toggleArrayFilter("difficulties", "hard");
      result.current.toggleArrayFilter("abilityTags", "communication");
    });

    await waitFor(() =>
      expect(questionBankService.pageQuestions).toHaveBeenLastCalledWith({
        pageNum: 1,
        pageSize: 20,
        status: "approved",
        collegeId: 9,
        majorId: 18,
        questionType: "专业题",
        abilityTag: "communication",
        difficulty: "hard",
      }),
    );

    await waitFor(() =>
      expect(questionBankService.getQuestionCoverage).toHaveBeenLastCalledWith({
        collegeId: 9,
        majorId: 18,
        interviewMode: "专业题",
        requiredCount: 4,
        abilityTag: "communication",
        difficulty: "hard",
      }),
    );
  });

  it("resets page number when filters change", async () => {
    vi.mocked(questionBankService.listColleges).mockResolvedValue([]);
    vi.mocked(questionBankService.listMajors).mockResolvedValue([]);
    vi.mocked(questionBankService.pageQuestions).mockResolvedValue({
      records: [],
      total: 0,
      size: 20,
      current: 1,
      pages: 3,
    });
    vi.mocked(questionBankService.getQuestionCoverage).mockResolvedValue({
      approvedCount: 1,
      exactMatchCount: 1,
      fallbackCount: 0,
      canStartImmediately: true,
      mayNeedAiGeneration: false,
    });

    const { result } = renderHook(() => useLobbyData("综合题", 5), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.setPageNum(3);
    });
    expect(result.current.pageNum).toBe(3);

    act(() => {
      result.current.toggleArrayFilter("questionTypes", "其他题");
    });

    expect(result.current.pageNum).toBe(1);
  });
});

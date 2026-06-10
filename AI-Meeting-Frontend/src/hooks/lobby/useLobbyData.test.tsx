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
      interviewMode: "专业认知",
      approvedCount: 4,
      exactMatchCount: 2,
      fallbackCount: 2,
      canStartImmediately: true,
      mayNeedAiGeneration: true,
    });

    const { result } = renderHook(() => useLobbyData("专业认知", 5), {
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
        interviewMode: "专业认知",
        requiredCount: 5,
      }),
    );
    await waitFor(() =>
      expect(result.current.coverage?.approvedCount).toBe(4),
    );
  });
});

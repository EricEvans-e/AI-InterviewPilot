import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import TeacherQuestionsPage from "@/pages/teacher/TeacherQuestionsPage";
import { useTeacherQuestions } from "@/hooks/teacher/useTeacherQuestions";
import { teacherService } from "@/services/teacherService";

vi.mock("@/hooks/teacher/useTeacherQuestions", () => ({
  useTeacherQuestions: vi.fn(),
}));

vi.mock("@/services/teacherService", () => ({
  teacherService: {
    listColleges: vi.fn(),
    listMajors: vi.fn(),
    deleteQuestion: vi.fn(),
    updateQuestionStatus: vi.fn(),
    batchUpdateQuestionStatus: vi.fn(),
  },
}));

vi.mock("@/components/teacher/QuestionBankTable", () => ({
  default: (props: Record<string, unknown>) => (
    <div data-testid="question-bank-table">
      <button onClick={() => (props.onApprove as (q: { id: number; title: string }) => void)?.({ id: 1, title: "Q1" })}>
        approve-one
      </button>
      <button onClick={() => (props.onReject as (q: { id: number; title: string }) => void)?.({ id: 2, title: "Q2" })}>
        reject-one
      </button>
      <button onClick={() => (props.onBatchApprove as (ids: number[]) => void)?.([1, 2])}>
        approve-batch
      </button>
      <button onClick={() => (props.onBatchReject as (ids: number[]) => void)?.([3, 4])}>
        reject-batch
      </button>
      <button onClick={() => (props.onBatchDelete as (ids: number[]) => void)?.([5, 6])}>
        delete-batch
      </button>
      <button onClick={() => (props.onBatchExpand as (ids: number[]) => void)?.([8, 9])}>
        expand-batch
      </button>
      <button onClick={() => (props.onSelectionChange as (ids: number[]) => void)?.([8, 9])}>
        select-items
      </button>
      <button onClick={() => (props.onClearSelection as () => void)?.()}>
        clear-selection
      </button>
      <div data-selected={JSON.stringify(props.selectedIds)} />
    </div>
  ),
}));

vi.mock("@/components/teacher/QuestionFormDialog", () => ({
  default: () => null,
}));

vi.mock("@/components/teacher/AiGenerateDialog", () => ({
  default: () => null,
}));

vi.mock("@/components/teacher/AiExpandDialog", () => ({
  default: (props: Record<string, unknown>) =>
    props.open ? (
      <div data-testid="ai-expand-dialog">
        {JSON.stringify({
          selectedQuestionIds: props.selectedQuestionIds,
          selectedQuestionTitles: (props.selectedQuestions as { title: string }[] | undefined)?.map(
            (item) => item.title,
          ),
        })}
      </div>
    ) : null,
}));

vi.mock("@/components/teacher/QuestionImportDialog", () => ({
  default: () => null,
}));

describe("TeacherQuestionsPage", () => {
  it("submits the full filter payload and clears it correctly", async () => {
    const updateFilters = vi.fn();
    vi.mocked(useTeacherQuestions).mockReturnValue({
      questions: [],
      total: 0,
      totalPages: 0,
      currentPage: 1,
      isLoading: false,
      isFetching: false,
      setPage: vi.fn(),
      refetch: vi.fn(),
      updateFilters,
      createQuestion: { mutate: vi.fn(), isPending: false },
      updateQuestion: { mutate: vi.fn(), isPending: false },
      deleteQuestion: { mutate: vi.fn() },
    } as never);

    vi.mocked(teacherService.listColleges).mockResolvedValue([
      { id: 1, name: "Test College" },
    ]);
    vi.mocked(teacherService.listMajors).mockResolvedValue([
      { id: 2, name: "Test Major" },
    ]);

    const { container } = render(<TeacherQuestionsPage />);

    await waitFor(() => expect(teacherService.listColleges).toHaveBeenCalled());

    const selects = Array.from(container.querySelectorAll("select"));
    fireEvent.change(screen.getByPlaceholderText("按题目标题搜索"), {
      target: { value: "  RoBERTa  " },
    });
    fireEvent.change(selects[0]!, { target: { value: "1" } });
    await waitFor(() => expect(teacherService.listMajors).toHaveBeenCalledWith(1));
    fireEvent.change(selects[1]!, { target: { value: "2" } });
    fireEvent.change(selects[2]!, { target: { value: "综合题" } });
    fireEvent.change(selects[3]!, { target: { value: "hard" } });
    fireEvent.change(selects[4]!, { target: { value: "approved" } });

    fireEvent.click(screen.getByRole("button", { name: "筛选" }));
    expect(updateFilters).toHaveBeenLastCalledWith({
      collegeId: 1,
      majorId: 2,
      titleKeyword: "RoBERTa",
      questionType: "综合题",
      difficulty: "hard",
      status: "approved",
    });

    fireEvent.click(screen.getByRole("button", { name: "清除" }));
    expect(updateFilters).toHaveBeenLastCalledWith({
      collegeId: undefined,
      majorId: undefined,
      titleKeyword: undefined,
      questionType: undefined,
      difficulty: undefined,
      status: undefined,
    });
  });

  it("reviews single and batch questions then refreshes and clears selection", async () => {
    const refetch = vi.fn();
    vi.mocked(useTeacherQuestions).mockReturnValue({
      questions: [],
      total: 0,
      totalPages: 0,
      currentPage: 1,
      isLoading: false,
      isFetching: false,
      setPage: vi.fn(),
      refetch,
      updateFilters: vi.fn(),
      createQuestion: { mutate: vi.fn(), isPending: false },
      updateQuestion: { mutate: vi.fn(), isPending: false },
      deleteQuestion: { mutate: vi.fn() },
    } as never);

    vi.mocked(teacherService.listColleges).mockResolvedValue([]);
    vi.mocked(teacherService.listMajors).mockResolvedValue([]);
    vi.mocked(teacherService.deleteQuestion).mockResolvedValue(undefined);
    vi.mocked(teacherService.updateQuestionStatus).mockResolvedValue(undefined);
    vi.mocked(teacherService.batchUpdateQuestionStatus).mockResolvedValue(undefined);
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    const alertSpy = vi.spyOn(window, "alert").mockImplementation(() => {});

    render(<TeacherQuestionsPage />);

    fireEvent.click(screen.getByText("approve-one"));
    await waitFor(() =>
      expect(teacherService.updateQuestionStatus).toHaveBeenCalledWith(1, "approved"),
    );

    fireEvent.click(screen.getByText("reject-one"));
    await waitFor(() =>
      expect(teacherService.updateQuestionStatus).toHaveBeenCalledWith(2, "rejected"),
    );

    fireEvent.click(screen.getByText("approve-batch"));
    await waitFor(() =>
      expect(teacherService.batchUpdateQuestionStatus).toHaveBeenCalledWith([1, 2], "approved"),
    );

    fireEvent.click(screen.getByText("reject-batch"));
    await waitFor(() =>
      expect(teacherService.batchUpdateQuestionStatus).toHaveBeenCalledWith([3, 4], "rejected"),
    );

    fireEvent.click(screen.getByText("delete-batch"));
    await waitFor(() =>
      expect(teacherService.deleteQuestion).toHaveBeenNthCalledWith(1, 5),
    );
    expect(teacherService.deleteQuestion).toHaveBeenNthCalledWith(2, 6);
    expect(confirmSpy).toHaveBeenCalled();

    await waitFor(() => expect(refetch).toHaveBeenCalledTimes(5));
    expect(alertSpy).toHaveBeenCalled();
  });

  it("opens AI expand dialog with selected questions from the current list", async () => {
    vi.mocked(useTeacherQuestions).mockReturnValue({
      questions: [
        { id: 8, title: "Sample Q8" },
        { id: 9, title: "Sample Q9" },
        { id: 10, title: "Sample Q10" },
      ],
      total: 3,
      totalPages: 1,
      currentPage: 1,
      isLoading: false,
      isFetching: false,
      setPage: vi.fn(),
      refetch: vi.fn(),
      updateFilters: vi.fn(),
      createQuestion: { mutate: vi.fn(), isPending: false },
      updateQuestion: { mutate: vi.fn(), isPending: false },
      deleteQuestion: { mutate: vi.fn() },
    } as never);

    vi.mocked(teacherService.listColleges).mockResolvedValue([]);
    vi.mocked(teacherService.listMajors).mockResolvedValue([]);

    render(<TeacherQuestionsPage />);

    fireEvent.click(screen.getByText("expand-batch"));

    const dialog = await screen.findByTestId("ai-expand-dialog");
    expect(dialog.textContent).toContain('"selectedQuestionIds":[8,9]');
    expect(dialog.textContent).toContain('"selectedQuestionTitles":["Sample Q8","Sample Q9"]');
  });
});

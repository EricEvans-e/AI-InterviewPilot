import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import QuestionImportDialog from "@/components/teacher/QuestionImportDialog";
import { teacherService } from "@/services/teacherService";

vi.mock("@/services/teacherService", () => ({
  teacherService: {
    listColleges: vi.fn(),
    listMajors: vi.fn(),
    previewQuestionImport: vi.fn(),
    confirmQuestionImport: vi.fn(),
  },
}));

describe("QuestionImportDialog", () => {
  it("keeps the dialog constrained and the preview pane scrollable", async () => {
    vi.mocked(teacherService.listColleges).mockResolvedValue([]);
    vi.mocked(teacherService.listMajors).mockResolvedValue([]);

    render(
      <QuestionImportDialog
        open
        onOpenChange={vi.fn()}
        onImported={vi.fn()}
      />,
    );

    const dialog = await screen.findByRole("dialog");
    expect(dialog.className).toContain("max-h-[90vh]");
    expect(dialog.className).toContain("overflow-hidden");
    expect(dialog.className).toContain("flex");

    const scrollArea = Array.from(document.querySelectorAll("div")).find((element) =>
      element.className.includes("h-[300px]"),
    );
    expect(scrollArea).toBeTruthy();
    expect(scrollArea?.className).toContain("min-h-0");
  });

  it("previews and confirms imported questions", async () => {
    vi.mocked(teacherService.listColleges).mockResolvedValue([
      { id: 11, name: "Test College" },
    ]);
    vi.mocked(teacherService.listMajors).mockResolvedValue([
      { id: 22, name: "Test Major" },
    ]);
    vi.mocked(teacherService.previewQuestionImport).mockResolvedValue({
      batchId: "imp-1",
      status: "PARTIAL_FAILED",
      totalRows: 2,
      validCount: 1,
      invalidCount: 1,
      importedCount: 0,
      items: [
        {
          rowIndex: 1,
          valid: true,
          title: "Explain the RoBERTa continued pretraining design",
          questionType: "professional",
        },
        {
          rowIndex: 2,
          valid: false,
          errorMessage: "content is required",
        },
      ],
      errors: [{ rowIndex: 2, message: "content is required" }],
    });
    vi.mocked(teacherService.confirmQuestionImport).mockResolvedValue({
      batchId: "imp-1",
      status: "IMPORTED",
      totalRows: 2,
      validCount: 1,
      invalidCount: 1,
      importedCount: 1,
      items: [],
      errors: [],
    });

    const onImported = vi.fn();

    render(
      <QuestionImportDialog
        open
        onOpenChange={vi.fn()}
        onImported={onImported}
      />,
    );

    await waitFor(() => expect(screen.getByText("Test College")).toBeTruthy());

    const selects = screen.getAllByRole("combobox");
    fireEvent.change(selects[1]!, { target: { value: "11" } });
    await waitFor(() => expect(screen.getByText("Test Major")).toBeTruthy());
    fireEvent.change(selects[2]!, { target: { value: "22" } });

    const fileInput = document.querySelector('input[type="file"]');
    expect(fileInput).toBeTruthy();
    fireEvent.change(fileInput!, {
      target: {
        files: [
          new File(["docx"], "questions.docx", {
            type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          }),
        ],
      },
    });

    fireEvent.click(screen.getAllByRole("button")[0]!);

    await waitFor(() =>
      expect(teacherService.previewQuestionImport).toHaveBeenCalledWith(
        expect.objectContaining({
          importType: "word_table",
          collegeId: 11,
          majorId: 22,
          defaultDifficulty: "medium",
          defaultYear: 2026,
          statusAfterImport: "pending_review",
        }),
      ),
    );
    expect(screen.getByText(/RoBERTa/i)).toBeTruthy();

    fireEvent.click(screen.getAllByRole("button")[2]!);

    await waitFor(() =>
      expect(teacherService.confirmQuestionImport).toHaveBeenCalledWith("imp-1"),
    );
    expect(onImported).toHaveBeenCalledTimes(1);
  });
});

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
  it("previews and confirms imported questions", async () => {
    vi.mocked(teacherService.listColleges).mockResolvedValue([
      { id: 11, name: "浙江测试学院" },
    ]);
    vi.mocked(teacherService.listMajors).mockResolvedValue([
      { id: 22, name: "人工智能" },
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
          title: "请说明 RoBERTa 继续预训练的设计思路",
          questionType: "专业题",
        },
        {
          rowIndex: 2,
          valid: false,
          errorMessage: "题目内容为空",
        },
      ],
      errors: [{ rowIndex: 2, message: "题目内容为空" }],
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

    await waitFor(() => expect(screen.getByText("浙江测试学院")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("院校"), {
      target: { value: "11" },
    });
    await waitFor(() => expect(screen.getByText("人工智能")).toBeTruthy());
    fireEvent.change(screen.getByLabelText("专业"), {
      target: { value: "22" },
    });
    fireEvent.change(screen.getByLabelText("Word 文件"), {
      target: {
        files: [
          new File(["docx"], "questions.docx", {
            type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          }),
        ],
      },
    });
    fireEvent.click(screen.getByRole("button", { name: "解析预览" }));

    await waitFor(() =>
      expect(teacherService.previewQuestionImport).toHaveBeenCalledWith(
        expect.objectContaining({
          importType: "word_table",
          collegeId: 11,
          majorId: 22,
          defaultQuestionType: "专业题",
          defaultDifficulty: "medium",
          defaultYear: 2026,
          statusAfterImport: "pending_review",
        }),
      ),
    );
    expect(screen.getByText("有效 1 道")).toBeTruthy();
    expect(screen.getByText("无效 1 道")).toBeTruthy();
    expect(screen.getByText("请说明 RoBERTa 继续预训练的设计思路")).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "确认导入 1 道题" }));

    await waitFor(() => expect(teacherService.confirmQuestionImport).toHaveBeenCalledWith("imp-1"));
    expect(onImported).toHaveBeenCalledTimes(1);
  });
});

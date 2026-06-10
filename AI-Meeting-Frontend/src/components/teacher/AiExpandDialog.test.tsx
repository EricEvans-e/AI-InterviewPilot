import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import AiExpandDialog from "@/components/teacher/AiExpandDialog";
import { teacherService } from "@/services/teacherService";

vi.mock("@/services/teacherService", () => ({
  teacherService: {
    listColleges: vi.fn(),
    listMajors: vi.fn(),
    getEnabledAiProperties: vi.fn(),
    aiExpandQuestions: vi.fn(),
  },
}));

describe("AiExpandDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(teacherService.listColleges).mockResolvedValue([]);
    vi.mocked(teacherService.listMajors).mockResolvedValue([]);
    vi.mocked(teacherService.getEnabledAiProperties).mockResolvedValue([]);
  });

  it("keeps the dialog constrained and the body scrollable", async () => {
    render(
      <AiExpandDialog
        open
        onOpenChange={vi.fn()}
        selectedQuestionIds={[11, 12]}
        selectedQuestions={[
          { id: 11, title: "样例题 1" },
          { id: 12, title: "样例题 2" },
        ]}
        onBatchSave={vi.fn()}
        isSaving={false}
      />,
    );

    const dialog = await screen.findByRole("dialog");
    expect(dialog.className).toContain("max-h-[90vh]");
    expect(dialog.className).toContain("overflow-hidden");
    expect(dialog.className).toContain("flex");

    const scrollContainer = Array.from(document.querySelectorAll("div")).find((element) =>
      element.className.includes("overflow-y-auto"),
    );
    expect(scrollContainer).toBeTruthy();
    expect(scrollContainer?.className).toContain("min-h-0");
    expect(scrollContainer?.className).toContain("flex-1");
  });

  it("submits manual expand params and saves generated questions as pending review", async () => {
    const onBatchSave = vi.fn();

    vi.mocked(teacherService.aiExpandQuestions).mockResolvedValue({
      questions: [
        {
          title: "扩展题 1",
          content: "请结合项目经历回答。",
          questionType: "综合题",
          difficulty: "hard",
        },
      ],
    });

    render(
      <AiExpandDialog
        open
        onOpenChange={vi.fn()}
        selectedQuestionIds={[11, 12]}
        selectedQuestions={[
          { id: 11, title: "样例题 1" },
          { id: 12, title: "样例题 2" },
        ]}
        onBatchSave={onBatchSave}
        isSaving={false}
      />,
    );

    await waitFor(() => expect(teacherService.listColleges).toHaveBeenCalled());

    const comboboxes = screen.getAllByRole("combobox");
    fireEvent.change(comboboxes[3]!, { target: { value: "综合题" } });
    fireEvent.change(comboboxes[4]!, { target: { value: "professional_knowledge" } });
    fireEvent.change(comboboxes[5]!, { target: { value: "hard" } });
    fireEvent.change(screen.getByRole("spinbutton"), { target: { value: "3" } });

    fireEvent.click(screen.getByLabelText("生成参考答案"));
    fireEvent.click(screen.getByLabelText("生成评分标准"));
    fireEvent.click(screen.getByRole("button", { name: "开始拓题" }));

    await waitFor(() =>
      expect(teacherService.aiExpandQuestions).toHaveBeenCalledWith({
        referenceQuestionIds: [11, 12],
        collegeId: undefined,
        majorId: undefined,
        questionType: "综合题",
        abilityTag: "professional_knowledge",
        count: 3,
        difficulty: "hard",
        generateReferenceAnswer: false,
        generateFollowUp: false,
        generateScoringRule: true,
        aiPropertiesId: undefined,
      }),
    );

    fireEvent.click(screen.getByRole("button", { name: "全部保存 (1)" }));

    expect(onBatchSave).toHaveBeenCalledWith([
      expect.objectContaining({
        title: "扩展题 1",
        isAiGenerated: true,
        status: "pending_review",
      }),
    ]);
  });

  it("allows clearing the count input before typing a new value", async () => {
    render(
      <AiExpandDialog
        open
        onOpenChange={vi.fn()}
        selectedQuestionIds={[11]}
        selectedQuestions={[{ id: 11, title: "样例题 1" }]}
        onBatchSave={vi.fn()}
        isSaving={false}
      />,
    );

    await waitFor(() => expect(teacherService.listColleges).toHaveBeenCalled());

    const countInput = screen.getByRole("spinbutton") as HTMLInputElement;
    fireEvent.change(countInput, { target: { value: "" } });
    expect(countInput.value).toBe("");

    fireEvent.change(countInput, { target: { value: "5" } });
    expect(countInput.value).toBe("5");

    fireEvent.blur(countInput);
    expect(countInput.value).toBe("5");
  });
});

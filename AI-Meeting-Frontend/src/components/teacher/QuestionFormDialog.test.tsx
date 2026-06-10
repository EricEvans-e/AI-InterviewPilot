import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import QuestionFormDialog from "@/components/teacher/QuestionFormDialog";
import { teacherService } from "@/services/teacherService";

vi.mock("@/services/teacherService", () => ({
  teacherService: {
    listColleges: vi.fn().mockResolvedValue([]),
    listMajors: vi.fn().mockResolvedValue([]),
  },
}));

describe("QuestionFormDialog", () => {
  it("keeps the dialog constrained and scrollable for long forms", async () => {
    render(
      <QuestionFormDialog
        open
        onOpenChange={vi.fn()}
        onSubmit={vi.fn()}
        isSubmitting={false}
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

    expect(teacherService.listColleges).toHaveBeenCalled();
  });

  it("keeps the answer time input available", async () => {
    render(
      <QuestionFormDialog
        open
        onOpenChange={vi.fn()}
        onSubmit={vi.fn()}
        isSubmitting={false}
      />,
    );

    await screen.findByRole("dialog");
    expect(document.querySelector('input[type="number"]')).toBeTruthy();
  });
});

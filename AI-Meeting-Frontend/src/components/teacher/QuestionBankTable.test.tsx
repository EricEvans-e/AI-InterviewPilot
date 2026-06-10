import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import QuestionBankTable from "@/components/teacher/QuestionBankTable";
import type { QuestionRespDTO } from "@/services/questionBankService";

const questions: QuestionRespDTO[] = [
  {
    id: 1,
    title: "Pending question",
    questionType: "综合题",
    collegeName: "Named College",
    majorName: "Named Major",
    difficulty: "medium",
    status: "pending_review",
    isAiGenerated: false,
  },
  {
    id: 2,
    title: "Approved question",
    questionType: "专业题",
    collegeName: "Second College",
    majorName: "Second Major",
    difficulty: "hard",
    status: "approved",
    isAiGenerated: true,
  },
  {
    id: 3,
    title: "Fallback question",
    questionType: "其他题",
    collegeId: 99,
    majorId: 199,
    difficulty: "easy",
    status: "draft",
    isAiGenerated: false,
  },
];

describe("QuestionBankTable", () => {
  it("renders provided names, lookup fallbacks, paging and review actions", () => {
    const onPageChange = vi.fn();
    const onEdit = vi.fn();
    const onDelete = vi.fn();
    const onApprove = vi.fn();
    const onReject = vi.fn();
    const onBatchApprove = vi.fn();
    const onBatchExpand = vi.fn();
    const onBatchDelete = vi.fn();
    const onBatchReject = vi.fn();
    const onSelectionChange = vi.fn();
    const onClearSelection = vi.fn();

    render(
      <QuestionBankTable
        questions={questions}
        colleges={[{ id: 99, name: "Fallback College" }]}
        majors={[{ id: 199, name: "Fallback Major", collegeId: 99 }]}
        total={25}
        currentPage={2}
        totalPages={5}
        isLoading={false}
        selectedIds={[1]}
        onSelectionChange={onSelectionChange}
        onClearSelection={onClearSelection}
        onBatchApprove={onBatchApprove}
        onBatchExpand={onBatchExpand}
        onBatchDelete={onBatchDelete}
        onBatchReject={onBatchReject}
        onApprove={onApprove}
        onReject={onReject}
        onPageChange={onPageChange}
        onEdit={onEdit}
        onDelete={onDelete}
      />,
    );

    expect(screen.getByText("Named College")).toBeTruthy();
    expect(screen.getByText("Named Major")).toBeTruthy();
    expect(screen.getByText("Fallback College")).toBeTruthy();
    expect(screen.getByText("Fallback Major")).toBeTruthy();
    expect(screen.getByRole("button", { name: "1" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "5" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "批量通过" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "AI 拓题" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "批量删除" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "通过" })).toBeTruthy();
    expect(screen.getByRole("button", { name: "拒绝" })).toBeTruthy();

    fireEvent.click(screen.getByRole("button", { name: "3" }));
    expect(onPageChange).toHaveBeenCalledWith(3);

    fireEvent.click(screen.getByRole("button", { name: "通过" }));
    expect(onApprove).toHaveBeenCalledWith(questions[0]);

    fireEvent.click(screen.getByRole("button", { name: "批量拒绝" }));
    expect(onBatchReject).toHaveBeenCalledWith([1]);

    fireEvent.click(screen.getByRole("button", { name: "批量删除" }));
    expect(onBatchDelete).toHaveBeenCalledWith([1]);

    fireEvent.click(screen.getByRole("button", { name: "AI 拓题" }));
    expect(onBatchExpand).toHaveBeenCalledWith([1]);

    fireEvent.click(screen.getByRole("checkbox", { name: "全选当前页题目" }));
    expect(onSelectionChange).toHaveBeenCalled();
  });
});

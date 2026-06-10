import { Check, ChevronLeft, ChevronRight, Pencil, Trash2, User, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type {
  CollegeRespDTO,
  MajorRespDTO,
  QuestionRespDTO,
} from "@/services/questionBankService";
import {
  QUESTION_TYPE_OPTIONS,
  DIFFICULTY_OPTIONS,
} from "@/services/questionBankService";

interface QuestionBankTableProps {
  questions: QuestionRespDTO[];
  colleges?: CollegeRespDTO[];
  majors?: MajorRespDTO[];
  total: number;
  currentPage: number;
  totalPages: number;
  isLoading: boolean;
  selectedIds: number[];
  onSelectionChange: (ids: number[]) => void;
  onClearSelection: () => void;
  onBatchApprove: (ids: number[]) => void;
  onBatchExpand: (ids: number[]) => void;
  onBatchDelete: (ids: number[]) => void;
  onBatchReject: (ids: number[]) => void;
  onApprove: (question: QuestionRespDTO) => void;
  onReject: (question: QuestionRespDTO) => void;
  onPageChange: (page: number) => void;
  onEdit: (question: QuestionRespDTO) => void;
  onDelete: (question: QuestionRespDTO) => void;
}

function getLabel(
  options: readonly { value: string; label: string }[],
  value?: string,
): string {
  if (!value) return "-";
  return options.find((o) => o.value === value)?.label ?? value;
}

function getDifficultyColor(value?: string): string {
  if (!value) return "";
  return DIFFICULTY_OPTIONS.find((d) => d.value === value)?.color ?? "";
}

function buildPageNumbers(currentPage: number, totalPages: number): number[] {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }
  const start = Math.max(1, currentPage - 2);
  const end = Math.min(totalPages, start + 4);
  const normalizedStart = Math.max(1, end - 4);
  return Array.from(
    { length: end - normalizedStart + 1 },
    (_, index) => normalizedStart + index,
  );
}

const STATUS_MAP: Record<string, { label: string; className: string }> = {
  approved: { label: "启用", className: "bg-green-100 text-green-700" },
  pending_review: { label: "待审核", className: "bg-blue-100 text-blue-700" },
  draft: { label: "草稿", className: "bg-yellow-100 text-yellow-700" },
  rejected: { label: "已拒绝", className: "bg-red-100 text-red-700" },
};

export default function QuestionBankTable({
  questions,
  colleges = [],
  majors = [],
  total,
  currentPage,
  totalPages,
  isLoading,
  selectedIds,
  onSelectionChange,
  onClearSelection,
  onBatchApprove,
  onBatchExpand,
  onBatchDelete,
  onBatchReject,
  onApprove,
  onReject,
  onPageChange,
  onEdit,
  onDelete,
}: QuestionBankTableProps) {
  const selectedIdSet = new Set(selectedIds);
  const currentPageIds = questions.map((q) => q.id);
  const collegeNameMap = new Map(colleges.map((college) => [college.id, college.name]));
  const majorNameMap = new Map(majors.map((major) => [major.id, major.name]));
  const allCurrentPageSelected =
    currentPageIds.length > 0 && currentPageIds.every((id) => selectedIdSet.has(id));
  const someCurrentPageSelected =
    currentPageIds.some((id) => selectedIdSet.has(id)) && !allCurrentPageSelected;
  const pageNumbers = buildPageNumbers(currentPage, totalPages);

  return (
    <div className="rounded-lg border border-slate-200 bg-white">
      {selectedIds.length > 0 && (
        <div className="flex items-center justify-between gap-3 border-b border-slate-200 bg-slate-50 px-4 py-3">
          <span className="text-sm text-slate-600">已选中 {selectedIds.length} 道题目</span>
          <div className="flex items-center gap-2">
            <Button size="sm" onClick={() => onBatchApprove(selectedIds)}>
              批量通过
            </Button>
            <Button variant="outline" size="sm" onClick={() => onBatchExpand(selectedIds)}>
              AI 拓题
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="text-red-600 hover:text-red-700"
              onClick={() => onBatchDelete(selectedIds)}
            >
              批量删除
            </Button>
            <Button variant="outline" size="sm" onClick={() => onBatchReject(selectedIds)}>
              批量拒绝
            </Button>
            <Button variant="ghost" size="sm" onClick={onClearSelection}>
              清空选择
            </Button>
          </div>
        </div>
      )}

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50 text-left">
              <th className="px-4 py-3">
                <input
                  aria-label="全选当前页题目"
                  type="checkbox"
                  className="h-4 w-4 rounded border-slate-300"
                  checked={allCurrentPageSelected}
                  ref={(node) => {
                    if (node) {
                      node.indeterminate = someCurrentPageSelected;
                    }
                  }}
                  onChange={(event) => {
                    if (event.target.checked) {
                      const merged = Array.from(new Set([...selectedIds, ...currentPageIds]));
                      onSelectionChange(merged);
                      return;
                    }
                    onSelectionChange(selectedIds.filter((id) => !currentPageIds.includes(id)));
                  }}
                />
              </th>
              <th className="px-4 py-3 font-medium text-slate-600">ID</th>
              <th className="px-4 py-3 font-medium text-slate-600">标题</th>
              <th className="px-4 py-3 font-medium text-slate-600">题型</th>
              <th className="px-4 py-3 font-medium text-slate-600">院校</th>
              <th className="px-4 py-3 font-medium text-slate-600">专业</th>
              <th className="px-4 py-3 font-medium text-slate-600">难度</th>
              <th className="px-4 py-3 font-medium text-slate-600">来源</th>
              <th className="px-4 py-3 font-medium text-slate-600">状态</th>
              <th className="px-4 py-3 font-medium text-slate-600">操作</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={10} className="px-4 py-12 text-center text-slate-400">
                  加载中...
                </td>
              </tr>
            ) : questions.length === 0 ? (
              <tr>
                <td colSpan={10} className="px-4 py-12 text-center text-slate-400">
                  暂无题目数据
                </td>
              </tr>
            ) : (
              questions.map((q) => {
                const status = STATUS_MAP[q.status ?? "draft"] ?? STATUS_MAP.draft;
                const collegeDisplayName =
                  q.collegeName ??
                  (q.collegeId ? collegeNameMap.get(q.collegeId) : undefined) ??
                  "-";
                const majorDisplayName =
                  q.majorName ??
                  (q.majorId ? majorNameMap.get(q.majorId) : undefined) ??
                  "-";
                return (
                  <tr
                    key={q.id}
                    className="border-b border-slate-100 last:border-0 hover:bg-slate-50/50"
                  >
                    <td className="px-4 py-3">
                      <input
                        aria-label={`选择题目 ${q.id}`}
                        type="checkbox"
                        className="h-4 w-4 rounded border-slate-300"
                        checked={selectedIdSet.has(q.id)}
                        onChange={(event) => {
                          if (event.target.checked) {
                            onSelectionChange([...selectedIds, q.id]);
                            return;
                          }
                          onSelectionChange(selectedIds.filter((id) => id !== q.id));
                        }}
                      />
                    </td>
                    <td className="px-4 py-3 text-slate-500">{q.id}</td>
                    <td className="max-w-[220px] truncate px-4 py-3 font-medium text-slate-800">
                      {q.title}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {getLabel(QUESTION_TYPE_OPTIONS, q.questionType)}
                    </td>
                    <td className="px-4 py-3 text-slate-600">{collegeDisplayName}</td>
                    <td className="px-4 py-3 text-slate-600">{majorDisplayName}</td>
                    <td className="px-4 py-3">
                      <span
                        className={cn(
                          "inline-block rounded-full px-2 py-0.5 text-xs font-medium",
                          getDifficultyColor(q.difficulty),
                        )}
                      >
                        {getLabel(DIFFICULTY_OPTIONS, q.difficulty)}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {q.isAiGenerated ? (
                        <span className="text-xs text-violet-600">AI</span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-xs text-slate-500">
                          <User className="h-3.5 w-3.5" /> 人工
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={cn(
                          "inline-block rounded-full px-2 py-0.5 text-xs font-medium",
                          status.className,
                        )}
                      >
                        {status.label}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1">
                        {q.status === "pending_review" && (
                          <>
                            <Button size="sm" variant="ghost" onClick={() => onApprove(q)}>
                              <Check className="mr-1 h-3.5 w-3.5" />
                              通过
                            </Button>
                            <Button size="sm" variant="ghost" onClick={() => onReject(q)}>
                              <X className="mr-1 h-3.5 w-3.5" />
                              拒绝
                            </Button>
                          </>
                        )}
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8"
                          onClick={() => onEdit(q)}
                          title="编辑"
                        >
                          <Pencil className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-red-500 hover:text-red-700"
                          onClick={() => onDelete(q)}
                          title="删除"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {total > 0 && (
        <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3">
          <span className="text-sm text-slate-500">共 {total} 条记录</span>
          <div className="flex items-center gap-1">
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage <= 1}
              onClick={() => onPageChange(currentPage - 1)}
              aria-label="上一页"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            {pageNumbers.map((page) => (
              <Button
                key={page}
                variant={page === currentPage ? "default" : "outline"}
                size="sm"
                onClick={() => onPageChange(page)}
              >
                {page}
              </Button>
            ))}
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage >= totalPages}
              onClick={() => onPageChange(currentPage + 1)}
              aria-label="下一页"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

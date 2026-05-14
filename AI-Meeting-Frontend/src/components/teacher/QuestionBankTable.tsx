import { Pencil, Trash2, Bot, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { QuestionRespDTO } from "@/services/questionBankService";
import {
  QUESTION_TYPE_OPTIONS,
  DIFFICULTY_OPTIONS,
} from "@/services/questionBankService";

interface QuestionBankTableProps {
  questions: QuestionRespDTO[];
  total: number;
  currentPage: number;
  totalPages: number;
  isLoading: boolean;
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
  return (
    DIFFICULTY_OPTIONS.find((d) => d.value === value)?.color ?? ""
  );
}

const STATUS_MAP: Record<string, { label: string; className: string }> = {
  active: { label: "启用", className: "bg-green-100 text-green-700" },
  inactive: { label: "停用", className: "bg-slate-100 text-slate-600" },
  draft: { label: "草稿", className: "bg-yellow-100 text-yellow-700" },
};

export default function QuestionBankTable({
  questions,
  total,
  currentPage,
  totalPages,
  isLoading,
  onPageChange,
  onEdit,
  onDelete,
}: QuestionBankTableProps) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white">
      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50 text-left">
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
                <td
                  colSpan={9}
                  className="px-4 py-12 text-center text-slate-400"
                >
                  加载中...
                </td>
              </tr>
            ) : questions.length === 0 ? (
              <tr>
                <td
                  colSpan={9}
                  className="px-4 py-12 text-center text-slate-400"
                >
                  暂无题目数据
                </td>
              </tr>
            ) : (
              questions.map((q) => {
                const status = STATUS_MAP[q.status ?? "active"] ?? STATUS_MAP.active;
                return (
                  <tr
                    key={q.id}
                    className="border-b border-slate-100 last:border-0 hover:bg-slate-50/50"
                  >
                    <td className="px-4 py-3 text-slate-500">{q.id}</td>
                    <td className="max-w-[200px] truncate px-4 py-3 font-medium text-slate-800">
                      {q.title}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {getLabel(QUESTION_TYPE_OPTIONS, q.questionType)}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {(q as unknown as Record<string, unknown>).collegeName as string ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {(q as unknown as Record<string, unknown>).majorName as string ?? "-"}
                    </td>
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
                        <span className="inline-flex items-center gap-1 text-xs text-violet-600">
                          <Bot className="h-3.5 w-3.5" /> AI
                        </span>
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

      {/* Pagination */}
      {total > 0 && (
        <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3">
          <span className="text-sm text-slate-500">
            共 {total} 条记录
          </span>
          <div className="flex items-center gap-1">
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage <= 1}
              onClick={() => onPageChange(currentPage - 1)}
            >
              上一页
            </Button>
            <span className="mx-2 text-sm text-slate-600">
              {currentPage} / {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage >= totalPages}
              onClick={() => onPageChange(currentPage + 1)}
            >
              下一页
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

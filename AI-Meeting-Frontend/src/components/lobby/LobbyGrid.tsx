import LobbyCard from "@/components/lobby/LobbyCard";
import { Button } from "@/components/ui/button";
import type {
  QuestionRespDTO,
  CollegeRespDTO,
  MajorRespDTO,
  QuestionCoverageResult,
} from "@/services/questionBankService";
import {
  Loader2,
  ChevronLeft,
  ChevronRight,
  Inbox,
  CheckCircle2,
  AlertTriangle,
} from "lucide-react";

interface LobbyGridProps {
  questions: QuestionRespDTO[];
  colleges: CollegeRespDTO[];
  majors: MajorRespDTO[];
  coverage?: QuestionCoverageResult;
  coverageLoading?: boolean;
  requiredCount: number;
  loading: boolean;
  pageNum: number;
  totalPages: number;
  totalQuestions: number;
  onPageChange: (page: number) => void;
  onStart: (questionId: number) => void;
}

export default function LobbyGrid({
  questions,
  colleges,
  majors,
  coverage,
  coverageLoading = false,
  requiredCount,
  loading,
  pageNum,
  totalPages,
  totalQuestions,
  onPageChange,
  onStart,
}: LobbyGridProps) {
  const collegeMap = new Map(colleges.map((c) => [c.id, c.name]));
  const majorMap = new Map(majors.map((m) => [m.id, m.name]));

  const coverageTone = coverage?.canStartImmediately ? "ok" : "warning";

  return (
    <div className="space-y-6">
      <div className="rounded-md border border-slate-200 bg-white p-4">
        {coverageLoading ? (
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <Loader2 className="h-4 w-4 animate-spin" />
            正在检查当前条件下的可用题量
          </div>
        ) : coverage ? (
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-start gap-2">
              {coverageTone === "ok" ? (
                <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" />
              ) : (
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-500" />
              )}
              <div>
                <p className="text-sm font-medium text-slate-800">
                  可用审核题 {coverage.approvedCount} 道
                  <span className="ml-2 text-xs font-normal text-slate-500">
                    本次建议 {requiredCount} 道
                  </span>
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  精确匹配 {coverage.exactMatchCount} 道，回退可用 {coverage.fallbackCount} 道
                  {coverage.mayNeedAiGeneration
                    ? "；题量偏少，建议老师继续导入或审核题目"
                    : "；题量充足，可直接开始练习"}
                </p>
              </div>
            </div>
          </div>
        ) : (
          <div className="text-sm text-slate-500">暂未获取题量覆盖情况</div>
        )}
      </div>

      {loading ? (
        <div className="flex min-h-[300px] items-center justify-center">
          <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
        </div>
      ) : questions.length === 0 ? (
        <div className="flex min-h-[300px] flex-col items-center justify-center gap-2 text-slate-400">
          <Inbox className="h-8 w-8" />
          <p className="text-sm">暂无符合条件的题目</p>
        </div>
      ) : (
        <>
          <div className="text-xs text-slate-400">
            共 {totalQuestions} 道题目
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {questions.map((question) => (
              <LobbyCard
                key={question.id}
                question={question}
                collegeName={
                  question.collegeName ??
                  (question.collegeId != null
                    ? collegeMap.get(question.collegeId)
                    : undefined)
                }
                majorName={
                  question.majorName ??
                  (question.majorId != null
                    ? majorMap.get(question.majorId)
                    : undefined)
                }
                onStart={onStart}
              />
            ))}
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={pageNum <= 1}
                onClick={() => onPageChange(pageNum - 1)}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-sm text-slate-500">
                {pageNum} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={pageNum >= totalPages}
                onClick={() => onPageChange(pageNum + 1)}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

import LobbyCard from "@/components/lobby/LobbyCard";
import { Button } from "@/components/ui/button";
import type { QuestionRespDTO, CollegeRespDTO, MajorRespDTO } from "@/services/questionBankService";
import { Loader2, ChevronLeft, ChevronRight, Inbox } from "lucide-react";

interface LobbyGridProps {
  questions: QuestionRespDTO[];
  colleges: CollegeRespDTO[];
  majors: MajorRespDTO[];
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
  loading,
  pageNum,
  totalPages,
  totalQuestions,
  onPageChange,
  onStart,
}: LobbyGridProps) {
  const collegeMap = new Map(colleges.map((c) => [c.id, c.name]));
  const majorMap = new Map(majors.map((m) => [m.id, m.name]));

  if (loading) {
    return (
      <div className="flex min-h-[300px] items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
      </div>
    );
  }

  if (questions.length === 0) {
    return (
      <div className="flex min-h-[300px] flex-col items-center justify-center gap-2 text-slate-400">
        <Inbox className="h-8 w-8" />
        <p className="text-sm">暂无符合条件的题目</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="text-xs text-slate-400">
        共 {totalQuestions} 道题目
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {questions.map((question) => (
          <LobbyCard
            key={question.id}
            question={question}
            collegeName={
              question.collegeId != null
                ? collegeMap.get(question.collegeId)
                : undefined
            }
            majorName={
              question.majorId != null
                ? majorMap.get(question.majorId)
                : undefined
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
    </div>
  );
}

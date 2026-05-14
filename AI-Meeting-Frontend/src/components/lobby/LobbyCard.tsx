import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { DIFFICULTY_OPTIONS, type QuestionRespDTO } from "@/services/questionBankService";
import { Clock, ArrowRight, Target } from "lucide-react";

interface LobbyCardProps {
  question: QuestionRespDTO;
  collegeName?: string;
  majorName?: string;
  onStart: (questionId: number) => void;
}

const formatDuration = (seconds?: number): string => {
  if (!seconds) return "不限时";
  if (seconds < 60) return `${seconds}秒`;
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return secs > 0 ? `${mins}分${secs}秒` : `${mins}分钟`;
};

export default function LobbyCard({
  question,
  collegeName,
  majorName,
  onStart,
}: LobbyCardProps) {
  const difficulty = DIFFICULTY_OPTIONS.find(
    (d) => d.value === question.difficulty,
  );

  return (
    <Card className="group flex flex-col border-slate-100 p-5 transition-shadow hover:shadow-md">
      <div className="flex-1 space-y-3">
        {/* Title & Difficulty */}
        <div className="flex items-start justify-between gap-2">
          <h3 className="text-sm font-medium leading-snug text-slate-900 line-clamp-2">
            {question.title}
          </h3>
          {difficulty && (
            <span
              className={cn(
                "shrink-0 rounded-full px-2 py-0.5 text-xs font-medium",
                difficulty.color,
              )}
            >
              {difficulty.label}
            </span>
          )}
        </div>

        {/* Meta info */}
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-slate-500">
          <span className="inline-flex items-center gap-1">
            <Clock className="h-3 w-3" />
            {formatDuration(question.answerTimeSeconds)}
          </span>
          {(collegeName || majorName) && (
            <span className="inline-flex items-center gap-1">
              <Target className="h-3 w-3" />
              {[collegeName, majorName].filter(Boolean).join(" / ")}
            </span>
          )}
        </div>

        {/* Question type tag */}
        {question.questionType && (
          <span className="inline-block rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-xs text-slate-500">
            {question.questionType}
          </span>
        )}
      </div>

      {/* Action */}
      <div className="mt-4 pt-3 border-t border-slate-100">
        <Button
          variant="ghost"
          size="sm"
          className="w-full justify-between text-slate-600 group-hover:text-slate-900 group-hover:bg-slate-50"
          onClick={() => onStart(question.id)}
        >
          开始模拟面试
          <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" />
        </Button>
      </div>
    </Card>
  );
}

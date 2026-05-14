import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import {
  QUESTION_TYPE_OPTIONS,
  DIFFICULTY_OPTIONS,
  ABILITY_TAG_OPTIONS,
  type CollegeRespDTO,
  type MajorRespDTO,
} from "@/services/questionBankService";
import type { LobbyFilters } from "@/hooks/lobby/useLobbyData";
import { Filter, RotateCcw } from "lucide-react";

interface LobbyFilterBarProps {
  filters: LobbyFilters;
  colleges: CollegeRespDTO[];
  collegesLoading: boolean;
  majors: MajorRespDTO[];
  majorsLoading: boolean;
  onCollegeChange: (collegeId: number | undefined) => void;
  onMajorChange: (majorId: number | undefined) => void;
  onToggleQuestionType: (value: string) => void;
  onToggleAbilityTag: (value: string) => void;
  onToggleDifficulty: (value: string) => void;
  onReset: () => void;
}

export default function LobbyFilterBar({
  filters,
  colleges,
  collegesLoading,
  majors,
  majorsLoading,
  onCollegeChange,
  onMajorChange,
  onToggleQuestionType,
  onToggleAbilityTag,
  onToggleDifficulty,
  onReset,
}: LobbyFilterBarProps) {
  const hasActiveFilters =
    filters.collegeId !== undefined ||
    filters.majorId !== undefined ||
    filters.questionTypes.length > 0 ||
    filters.abilityTags.length > 0 ||
    filters.difficulties.length > 0;

  return (
    <Card className="border-slate-100 p-5 space-y-5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm font-medium text-slate-900">
          <Filter className="h-4 w-4 text-slate-400" />
          筛选条件
        </div>
        {hasActiveFilters && (
          <Button
            variant="ghost"
            size="sm"
            className="h-7 gap-1 text-xs text-slate-500"
            onClick={onReset}
          >
            <RotateCcw className="h-3 w-3" />
            重置
          </Button>
        )}
      </div>

      {/* College & Major dropdowns */}
      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="flex-1 space-y-1.5">
          <label className="text-xs font-medium text-slate-500">目标院校</label>
          <select
            className="w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition-colors focus:border-slate-400 focus:ring-1 focus:ring-slate-400 disabled:opacity-50"
            value={filters.collegeId ?? ""}
            disabled={collegesLoading}
            onChange={(e) => {
              const val = e.target.value;
              onCollegeChange(val ? Number(val) : undefined);
            }}
          >
            <option value="">全部院校</option>
            {colleges.map((college) => (
              <option key={college.id} value={college.id}>
                {college.name}
              </option>
            ))}
          </select>
        </div>

        <div className="flex-1 space-y-1.5">
          <label className="text-xs font-medium text-slate-500">目标专业</label>
          <select
            className="w-full rounded-md border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition-colors focus:border-slate-400 focus:ring-1 focus:ring-slate-400 disabled:opacity-50"
            value={filters.majorId ?? ""}
            disabled={majorsLoading}
            onChange={(e) => {
              const val = e.target.value;
              onMajorChange(val ? Number(val) : undefined);
            }}
          >
            <option value="">全部专业</option>
            {majors.map((major) => (
              <option key={major.id} value={major.id}>
                {major.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Question type chips */}
      <div className="space-y-1.5">
        <label className="text-xs font-medium text-slate-500">题型</label>
        <div className="flex flex-wrap gap-2">
          {QUESTION_TYPE_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              className={cn(
                "rounded-full border px-3 py-1 text-xs font-medium transition-colors",
                filters.questionTypes.includes(opt.value)
                  ? "border-slate-900 bg-slate-900 text-white"
                  : "border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-50",
              )}
              onClick={() => onToggleQuestionType(opt.value)}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* Ability tag chips */}
      <div className="space-y-1.5">
        <label className="text-xs font-medium text-slate-500">能力标签</label>
        <div className="flex flex-wrap gap-2">
          {ABILITY_TAG_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              className={cn(
                "rounded-full border px-3 py-1 text-xs font-medium transition-colors",
                filters.abilityTags.includes(opt.value)
                  ? "border-slate-900 bg-slate-900 text-white"
                  : "border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-50",
              )}
              onClick={() => onToggleAbilityTag(opt.value)}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>

      {/* Difficulty chips */}
      <div className="space-y-1.5">
        <label className="text-xs font-medium text-slate-500">难度</label>
        <div className="flex flex-wrap gap-2">
          {DIFFICULTY_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              className={cn(
                "rounded-full border px-3 py-1 text-xs font-medium transition-colors",
                filters.difficulties.includes(opt.value)
                  ? "border-slate-900 bg-slate-900 text-white"
                  : "border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-50",
              )}
              onClick={() => onToggleDifficulty(opt.value)}
            >
              {opt.label}
            </button>
          ))}
        </div>
      </div>
    </Card>
  );
}

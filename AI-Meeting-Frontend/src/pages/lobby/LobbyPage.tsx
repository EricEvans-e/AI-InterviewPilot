import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card } from "@/components/ui/card";
import LobbyFilterBar from "@/components/lobby/LobbyFilterBar";
import LobbyGrid from "@/components/lobby/LobbyGrid";
import { useLobbyData } from "@/hooks/lobby/useLobbyData";
import { interviewService } from "@/services/interviewService";
import { ROUTES } from "@/lib/constants";
import { Shuffle, Timer, Loader2 } from "lucide-react";

const QUICK_MODES = [
  {
    key: "practice",
    label: "开始练习",
    description: "基于当前筛选条件抽取 5 道题，适合日常练习。",
    icon: Shuffle,
    interviewMode: "综合题",
    questionCount: 5,
  },
  {
    key: "full",
    label: "全真模拟",
    description: "基于当前筛选条件抽取 10 道题，适合完整模拟。",
    icon: Timer,
    interviewMode: "综合题",
    questionCount: 10,
  },
] as const;

export default function LobbyPage() {
  const navigate = useNavigate();
  const [startingMode, setStartingMode] = useState<string | null>(null);
  const selectedInterviewMode = "综合题";
  const selectedCardCount = 5;
  const lobbyData = useLobbyData(selectedInterviewMode, selectedCardCount);

  const selectedCardMode =
    lobbyData.filters.questionTypes.length === 1
      ? lobbyData.filters.questionTypes[0]
      : selectedInterviewMode;

  const handleStartFromBank = useCallback(
    async (params: {
      interviewMode: string;
      questionCount?: number;
      collegeId?: number;
      majorId?: number;
      difficulty?: string;
      abilityTag?: string;
    }) => {
      try {
        const result = await interviewService.createSessionFromBank(params);
        navigate(`${ROUTES.interviewBank}/${encodeURIComponent(result.sessionId)}`);
      } catch (error) {
        const message =
          error instanceof Error ? error.message : "创建面试失败，请重试";
        alert(message);
      }
    },
    [navigate],
  );

  const handleQuickMode = useCallback(
    async (mode: (typeof QUICK_MODES)[number]) => {
      setStartingMode(mode.key);
      try {
        await handleStartFromBank({
          interviewMode: mode.interviewMode,
          questionCount: mode.questionCount,
          collegeId: lobbyData.filters.collegeId,
          majorId: lobbyData.filters.majorId,
          difficulty:
            lobbyData.filters.difficulties.length === 1
              ? lobbyData.filters.difficulties[0]
              : undefined,
          abilityTag:
            lobbyData.filters.abilityTags.length === 1
              ? lobbyData.filters.abilityTags[0]
              : undefined,
        });
      } finally {
        setStartingMode(null);
      }
    },
    [handleStartFromBank, lobbyData.filters],
  );

  const handleStartFromCard = useCallback(async () => {
    setStartingMode("card");
    try {
      await handleStartFromBank({
        interviewMode: selectedCardMode,
        questionCount: selectedCardCount,
        collegeId: lobbyData.filters.collegeId,
        majorId: lobbyData.filters.majorId,
        difficulty:
          lobbyData.filters.difficulties.length === 1
            ? lobbyData.filters.difficulties[0]
            : undefined,
        abilityTag:
          lobbyData.filters.abilityTags.length === 1
            ? lobbyData.filters.abilityTags[0]
            : undefined,
      });
    } finally {
      setStartingMode(null);
    }
  }, [handleStartFromBank, lobbyData.filters, selectedCardMode]);

  return (
    <div className="h-full overflow-y-auto bg-white">
      <div className="mx-auto max-w-6xl space-y-8 px-6 py-10">
        <div className="space-y-2">
          <h1 className="text-2xl font-semibold text-slate-900">题库面试大厅</h1>
          <p className="text-sm text-slate-500">
            先用左侧筛选院校、专业、题型和难度，再选择练习方式开始模拟。
          </p>
        </div>

        <div className="rounded-md border border-slate-200 bg-slate-50 px-4 py-3">
          <p className="text-sm font-medium text-slate-900">练习方式说明</p>
          <div className="mt-2 space-y-1 text-sm text-slate-600">
            <p>开始练习会基于当前筛选条件抽取 5 道题，适合日常练习。</p>
            <p>全真模拟会基于当前筛选条件抽取 10 道题，适合完整模拟。</p>
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          {QUICK_MODES.map((mode) => {
            const Icon = mode.icon;
            const isStarting = startingMode === mode.key;
            return (
              <Card
                key={mode.key}
                className="group cursor-pointer border-slate-100 p-4 transition-all hover:border-slate-300 hover:shadow-md"
                onClick={() => {
                  if (!startingMode) {
                    void handleQuickMode(mode);
                  }
                }}
              >
                <div className="space-y-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 text-slate-600 transition-colors group-hover:bg-slate-900 group-hover:text-white">
                    {isStarting ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Icon className="h-4 w-4" />
                    )}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-slate-900">
                      {mode.label}
                    </p>
                    <p className="mt-0.5 text-xs text-slate-500">
                      {mode.description}
                    </p>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>

        <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
          <LobbyFilterBar
            filters={lobbyData.filters}
            colleges={lobbyData.colleges}
            collegesLoading={lobbyData.collegesLoading}
            majors={lobbyData.majors}
            majorsLoading={lobbyData.majorsLoading}
            onCollegeChange={(collegeId) =>
              lobbyData.updateFilters({ collegeId, majorId: undefined })
            }
            onMajorChange={(majorId) => lobbyData.updateFilters({ majorId })}
            onToggleQuestionType={(value) =>
              lobbyData.toggleArrayFilter("questionTypes", value)
            }
            onToggleAbilityTag={(value) =>
              lobbyData.toggleArrayFilter("abilityTags", value)
            }
            onToggleDifficulty={(value) =>
              lobbyData.toggleArrayFilter("difficulties", value)
            }
            onReset={lobbyData.resetFilters}
          />

          <LobbyGrid
            questions={lobbyData.questions}
            colleges={lobbyData.colleges}
            majors={lobbyData.majors}
            coverage={lobbyData.coverage}
            coverageLoading={lobbyData.coverageLoading}
            requiredCount={selectedCardCount}
            loading={lobbyData.questionsLoading}
            pageNum={lobbyData.pageNum}
            totalPages={lobbyData.totalPages}
            totalQuestions={lobbyData.totalQuestions}
            onPageChange={lobbyData.setPageNum}
            onStart={() => {
              if (lobbyData.coverage && !lobbyData.coverage.canStartImmediately) {
                alert("当前筛选条件下暂无可用审核题目，请先让老师导入并审核题库。");
                return;
              }
              void handleStartFromCard();
            }}
          />
        </div>
      </div>
    </div>
  );
}

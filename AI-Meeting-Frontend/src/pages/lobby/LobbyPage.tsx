import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card } from "@/components/ui/card";
import LobbyFilterBar from "@/components/lobby/LobbyFilterBar";
import LobbyGrid from "@/components/lobby/LobbyGrid";
import { useLobbyData } from "@/hooks/lobby/useLobbyData";
import { interviewService } from "@/services/interviewService";
import { ROUTES } from "@/lib/constants";
import {
  Shuffle,
  Timer,
  GraduationCap,
  Briefcase,
  Layers,
  Loader2,
} from "lucide-react";

const QUICK_MODES = [
  {
    key: "random",
    label: "随机模拟",
    description: "随机抽取题目，快速进入面试",
    icon: Shuffle,
    interviewMode: "综合素质",
    questionCount: 5,
  },
  {
    key: "full",
    label: "全真模拟",
    description: "固定题量 + 严格计时，还原真实考场",
    icon: Timer,
    interviewMode: "综合素质",
    questionCount: 10,
  },
  {
    key: "college",
    label: "按院校备考",
    description: "选择目标院校，针对性练习",
    icon: GraduationCap,
    interviewMode: "综合素质",
    questionCount: 5,
  },
  {
    key: "major",
    label: "按专业备考",
    description: "选择目标专业，专项突破",
    icon: Briefcase,
    interviewMode: "专业认知",
    questionCount: 5,
  },
  {
    key: "type",
    label: "按题型练习",
    description: "选择题型，逐个击破",
    icon: Layers,
    interviewMode: "半结构化",
    questionCount: 5,
  },
] as const;

export default function LobbyPage() {
  const navigate = useNavigate();
  const [startingMode, setStartingMode] = useState<string | null>(null);
  const selectedInterviewMode = "综合素质";
  const selectedCardCount = 4;
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
    }) => {
      try {
        const result = await interviewService.createSessionFromBank(params);
        // Navigate to question-bank specific interview page
        navigate(
          `${ROUTES.interviewBank}/${encodeURIComponent(result.sessionId)}`,
        );
      } catch (error) {
        const message =
          error instanceof Error
            ? error.message
            : "创建面试失败，请重试";
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
        });
      } finally {
        setStartingMode(null);
      }
    },
    [handleStartFromBank, lobbyData.filters],
  );

  const handleStartFromCard = useCallback(
    async () => {
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
        });
      } finally {
        setStartingMode(null);
      }
    },
    [handleStartFromBank, lobbyData.filters, selectedCardMode],
  );

  return (
    <div className="h-full overflow-y-auto bg-white">
      <div className="max-w-6xl mx-auto px-6 py-10 space-y-8">
        {/* Header */}
        <div className="space-y-2">
          <h1 className="text-2xl font-semibold text-slate-900">题库面试大厅</h1>
          <p className="text-sm text-slate-500">
            选择模拟方式，开始你的面试练习。
          </p>
        </div>

        {/* Quick entry modes */}
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
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

        {/* Main content: filters + grid */}
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
            onMajorChange={(majorId) =>
              lobbyData.updateFilters({ majorId })
            }
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

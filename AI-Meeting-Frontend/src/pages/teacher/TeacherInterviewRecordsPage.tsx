import { useState, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ClipboardList } from "lucide-react";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useInterviewRecords } from "@/hooks/teacher/useInterviewRecords";
import { useTeacherInterviewReport } from "@/hooks/teacher/useTeacherInterviewReport";
import TeacherReviewForm from "@/components/teacher/TeacherReviewForm";
import InterviewScoreAndRadarCard from "@/components/interview/report/InterviewScoreAndRadarCard";
import InterviewQaReplayCard from "@/components/interview/report/InterviewQaReplayCard";
import InterviewNextActionsCard from "@/components/interview/report/InterviewNextActionsCard";
import InterviewConclusionCard from "@/components/interview/report/InterviewConclusionCard";
import InterviewTeacherReviewCard from "@/components/interview/report/InterviewTeacherReviewCard";
import type { InterviewRecordDTO, ReviewSubmitDTO } from "@/services/teacherService";

const STATUS_MAP: Record<string, { label: string; className: string }> = {
  FINISHED: { label: "已完成", className: "bg-green-100 text-green-700" },
  EVALUATED: { label: "已评估", className: "bg-emerald-100 text-emerald-700" },
  IN_PROGRESS: { label: "进行中", className: "bg-blue-100 text-blue-700" },
  INIT: { label: "待开始", className: "bg-slate-100 text-slate-600" },
  DRAFT: { label: "草稿", className: "bg-amber-100 text-amber-700" },
};

export default function TeacherInterviewRecordsPage() {
  const {
    records,
    total,
    totalPages,
    currentPage,
    isLoading,
    isFetching,
    setPage,
    submitReview,
  } = useInterviewRecords();

  const [selectedRecord, setSelectedRecord] = useState<InterviewRecordDTO | null>(null);

  const handleSelect = useCallback((record: InterviewRecordDTO) => {
    setSelectedRecord((prev) => (prev?.id === record.id ? null : record));
  }, []);

  const handleReviewSubmit = useCallback(
    (data: ReviewSubmitDTO) => {
      submitReview.mutate(data, {
        onSuccess: () => {
          window.alert("点评提交成功");
        },
        onError: () => {
          window.alert("点评提交失败，请重试");
        },
      });
    },
    [submitReview],
  );

  const formatTime = (time?: string) => {
    if (!time) return "-";
    try {
      return new Date(time).toLocaleString("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return time;
    }
  };

  const formatDuration = (seconds?: number) => {
    if (seconds == null) return "-";
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return m > 0 ? `${m}分${s}秒` : `${s}秒`;
  };

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">面试记录</h1>
          <p className="mt-0.5 text-sm text-slate-500">
            浏览所有学生的面试记录并提交教师点评
          </p>
        </div>
      </div>

      {/* Main content */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left: Record list */}
        <div
          className={cn(
            "flex flex-col overflow-hidden border-r border-slate-200 transition-all",
            selectedRecord ? "w-[40%]" : "w-full",
          )}
        >
          <div className="flex-1 overflow-auto">
            <div className="rounded-lg border border-slate-200 bg-white">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-200 bg-slate-50 text-left">
                      <th className="px-4 py-3 font-medium text-slate-600">会话ID</th>
                      <th className="px-4 py-3 font-medium text-slate-600">面试分数</th>
                      <th className="px-4 py-3 font-medium text-slate-600">状态</th>
                      <th className="px-4 py-3 font-medium text-slate-600">题数</th>
                      <th className="px-4 py-3 font-medium text-slate-600">时长</th>
                      <th className="px-4 py-3 font-medium text-slate-600">创建时间</th>
                      <th className="px-4 py-3 font-medium text-slate-600">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {isLoading || isFetching ? (
                      <tr>
                        <td colSpan={7} className="px-4 py-12 text-center text-slate-400">
                          加载中...
                        </td>
                      </tr>
                    ) : records.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="px-4 py-12 text-center text-slate-400">
                          暂无面试记录
                        </td>
                      </tr>
                    ) : (
                      records.map((record) => {
                        const status = STATUS_MAP[record.interviewStatus ?? ""] ?? {
                          label: record.interviewStatus ?? "-",
                          className: "bg-slate-100 text-slate-600",
                        };
                        const isSelected = selectedRecord?.id === record.id;
                        return (
                          <tr
                            key={record.id}
                            className={cn(
                              "border-b border-slate-100 last:border-0 cursor-pointer transition-colors",
                              isSelected
                                ? "bg-indigo-50/60"
                                : "hover:bg-slate-50/50",
                            )}
                            onClick={() => handleSelect(record)}
                          >
                            <td className="max-w-[140px] truncate px-4 py-3 font-mono text-xs text-slate-600">
                              {record.sessionId ?? "-"}
                            </td>
                            <td className="px-4 py-3">
                              <span className="font-semibold text-indigo-600">
                                {record.interviewScore ?? "-"}
                              </span>
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
                            <td className="px-4 py-3 text-slate-600">
                              {record.questionCount ?? "-"}
                            </td>
                            <td className="px-4 py-3 text-slate-600">
                              {formatDuration(record.durationSeconds)}
                            </td>
                            <td className="px-4 py-3 text-slate-500">
                              {formatTime(record.createTime)}
                            </td>
                            <td className="px-4 py-3">
                              <Button
                                variant="ghost"
                                size="sm"
                                className="h-7 gap-1 text-xs"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleSelect(record);
                                }}
                              >
                                <ClipboardList className="h-3.5 w-3.5" />
                                查看
                              </Button>
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
                  <span className="text-sm text-slate-500">共 {total} 条记录</span>
                  <div className="flex items-center gap-1">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={currentPage <= 1}
                      onClick={() => setPage(currentPage - 1)}
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
                      onClick={() => setPage(currentPage + 1)}
                    >
                      下一页
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right: Full report panel */}
        <AnimatePresence mode="wait">
          {selectedRecord ? (
            <motion.div
              key={selectedRecord.id}
              className="w-[60%] overflow-hidden"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }}
            >
              <ReportDetailPanel
                sessionId={selectedRecord.sessionId ?? null}
                isSubmitting={submitReview.isPending}
                onReviewSubmit={handleReviewSubmit}
              />
            </motion.div>
          ) : null}
        </AnimatePresence>
      </div>
    </div>
  );
}

// ── Report detail panel ──

function ReportDetailPanel({
  sessionId,
  isSubmitting,
  onReviewSubmit,
}: {
  sessionId: string | null;
  isSubmitting: boolean;
  onReviewSubmit: (data: ReviewSubmitDTO) => void;
}) {
  const {
    isRecordLoading,
    recordError,
    resumeScore,
    interviewScore,
    compositeScore,
    isCompositeEstimated,
    radarPoints,
    dimensionScores,
    sortedSuggestions,
    interviewDirection,
    qaReviews,
    reviewFeedback,
  } = useTeacherInterviewReport(sessionId);

  return (
    <ScrollArea className="h-full">
      <div className="space-y-5 p-5">
        {/* Score + Radar */}
        <InterviewScoreAndRadarCard
          resumeScore={resumeScore}
          interviewScore={interviewScore}
          compositeScore={compositeScore}
          isCompositeEstimated={isCompositeEstimated}
          radarPoints={radarPoints}
          dimensionScores={dimensionScores}
        />

        {/* Q&A Replay */}
        <InterviewQaReplayCard
          qaReviews={qaReviews}
          isRecordLoading={isRecordLoading}
          recordError={recordError}
        />

        {/* Suggestions + Conclusion */}
        <div className="grid items-start gap-5 lg:grid-cols-[1.2fr_0.8fr]">
          <InterviewNextActionsCard
            reviewFeedback={reviewFeedback}
            sortedSuggestions={sortedSuggestions}
            isRecordLoading={isRecordLoading}
            recordError={recordError}
          />
          <InterviewConclusionCard
            interviewDirection={interviewDirection}
            reviewFeedback={reviewFeedback}
            isRecordLoading={isRecordLoading}
            recordError={recordError}
          />
        </div>

        {/* Existing teacher reviews */}
        <InterviewTeacherReviewCard sessionId={sessionId} />

        {/* Review form */}
        {sessionId && (
          <TeacherReviewForm
            sessionId={sessionId}
            isSubmitting={isSubmitting}
            onSubmit={onReviewSubmit}
          />
        )}
      </div>
    </ScrollArea>
  );
}

import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { teacherService } from "@/services/teacherService";

type InterviewTeacherReviewCardProps = {
  sessionId: string | null;
};

const formatDate = (dateStr?: string) => {
  if (!dateStr) return "";
  try {
    const date = new Date(dateStr);
    if (Number.isNaN(date.getTime())) return "";
    return date.toLocaleDateString("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  } catch {
    return "";
  }
};

export default function InterviewTeacherReviewCard({
  sessionId,
}: InterviewTeacherReviewCardProps) {
  const { data: reviews, isLoading } = useQuery({
    queryKey: ["teacher-reviews", sessionId],
    enabled: Boolean(sessionId),
    queryFn: () => teacherService.getSessionReviews(sessionId as string),
    retry: false,
    refetchOnWindowFocus: false,
    staleTime: 60_000,
  });

  return (
    <Card className="border-slate-100 p-6">
      <p className="text-sm font-medium text-slate-900">教师点评</p>

      {!sessionId ? (
        <div className="mt-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          暂无教师点评
        </div>
      ) : isLoading ? (
        <div className="mt-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          正在加载教师点评...
        </div>
      ) : !reviews || reviews.length === 0 ? (
        <div className="mt-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          暂无教师点评
        </div>
      ) : (
        <div className="mt-4 space-y-4">
          {reviews.map((review, index) => (
            <motion.div
              key={review.id}
              className="rounded-lg border border-slate-100 p-4"
              initial={{ opacity: 0, y: 12, filter: "blur(4px)" }}
              animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              transition={{
                duration: 0.28,
                delay: index * 0.06,
                ease: [0.22, 1, 0.36, 1],
              }}
            >
              {/* Header: teacher info + date */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-medium text-slate-700">
                    教师 #{review.teacherId ?? "未知"}
                  </span>
                  {review.isExcellentSample && (
                    <span className="inline-flex items-center rounded-full border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-[10px] font-medium text-emerald-700">
                      优秀样本
                    </span>
                  )}
                  {review.isModelMisjudge && (
                    <span className="inline-flex items-center rounded-full border border-rose-200 bg-rose-50 px-2 py-0.5 text-[10px] font-medium text-rose-700">
                      模型误判
                    </span>
                  )}
                </div>
                {review.createTime && (
                  <span className="text-[10px] text-slate-400">
                    {formatDate(review.createTime)}
                  </span>
                )}
              </div>

              {/* Score adjustment */}
              {review.adjustedScore != null && review.adjustedScore !== 0 && (
                <div className="mt-2">
                  <span
                    className={cn(
                      "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
                      review.adjustedScore > 0
                        ? "bg-emerald-50 text-emerald-700"
                        : "bg-rose-50 text-rose-700",
                    )}
                  >
                    分数调整: {review.adjustedScore > 0 ? "+" : ""}
                    {review.adjustedScore}
                  </span>
                </div>
              )}

              {/* Comment */}
              {review.content && (
                <p className="mt-3 text-sm leading-6 text-slate-600">
                  {review.content}
                </p>
              )}
            </motion.div>
          ))}
        </div>
      )}
    </Card>
  );
}

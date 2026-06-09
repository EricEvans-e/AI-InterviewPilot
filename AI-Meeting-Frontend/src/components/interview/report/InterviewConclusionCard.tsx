import { motion } from "framer-motion";
import { Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import type { ReviewFeedback } from "@/components/interview/report/types";

type InterviewConclusionCardProps = {
  interviewDirection: string | null;
  reviewFeedback: ReviewFeedback;
  isRecordLoading: boolean;
  recordError: string | null;
  canGenerate?: boolean;
  isGenerating?: boolean;
  onGenerate?: (() => void | Promise<unknown>) | undefined;
};

const renderList = (items: string[]) => (
  <div className="space-y-2">
    {items.map((item, index) => (
      <motion.div
        key={`${index}-${item}`}
        className="rounded-2xl bg-slate-50 px-4 py-3"
        initial={{ opacity: 0, y: 12, filter: "blur(4px)" }}
        animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
        transition={{
          duration: 0.28,
          delay: index * 0.06,
          ease: [0.22, 1, 0.36, 1],
        }}
      >
        <p className="text-sm leading-6 text-slate-700">{item}</p>
      </motion.div>
    ))}
  </div>
);

export default function InterviewConclusionCard({
  interviewDirection,
  reviewFeedback,
  isRecordLoading,
  recordError,
  canGenerate = false,
  isGenerating = false,
  onGenerate,
}: InterviewConclusionCardProps) {
  const hasContent =
    Boolean(reviewFeedback.overallComment) ||
    reviewFeedback.highlights.length > 0 ||
    reviewFeedback.improvementTips.length > 0;

  return (
    <Card className="border-slate-100 p-6">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-900">面试结论</p>
          <p className="mt-1 text-xs text-slate-500">
            面试方向
            {interviewDirection ? `：${interviewDirection}` : "暂无数据"}
          </p>
        </div>
        {canGenerate && onGenerate ? (
          <Button
            type="button"
            size="sm"
            className="h-8 rounded-full px-3 text-xs"
            onClick={() => void onGenerate()}
            disabled={isGenerating}
          >
            <Sparkles className="h-3.5 w-3.5" />
            {isGenerating ? "生成中..." : "生成 AI 结论"}
          </Button>
        ) : null}
      </div>

      {isGenerating && !isRecordLoading && !recordError ? (
        <div className="mt-4 rounded-2xl border border-indigo-100 bg-indigo-50 px-4 py-3 text-sm text-indigo-700">
          正在生成 AI 面试结论，完成后当前页面会自动更新。
        </div>
      ) : null}

      {isRecordLoading ? (
        <div className="mt-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
          正在生成面试结论...
        </div>
      ) : null}

      {recordError ? (
        <div className="mt-4 rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-600">
          {recordError}
        </div>
      ) : null}

      {!isRecordLoading && !recordError && !hasContent ? (
        <div className="mt-4 rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
          暂无面试结论
        </div>
      ) : null}

      {!isRecordLoading && !recordError && hasContent ? (
        <div className="mt-5 space-y-5">
          <section className="space-y-2">
            <p className="text-xs font-medium uppercase tracking-[0.08em] text-slate-500">
              总体评价
            </p>
            <div className="rounded-2xl bg-slate-50 px-4 py-4">
              <p className="text-sm leading-7 text-slate-700">
                {reviewFeedback.overallComment || "暂无总体评价"}
              </p>
            </div>
          </section>

          <Separator />

          <section className="space-y-3">
            <p className="text-xs font-medium uppercase tracking-[0.08em] text-slate-500">
              亮点总结
            </p>
            {reviewFeedback.highlights.length > 0 ? (
              renderList(reviewFeedback.highlights)
            ) : (
              <div className="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-400">
                暂无亮点总结
              </div>
            )}
          </section>

          <Separator />

          <section className="space-y-3">
            <p className="text-xs font-medium uppercase tracking-[0.08em] text-slate-500">
              待改进点
            </p>
            {reviewFeedback.improvementTips.length > 0 ? (
              renderList(reviewFeedback.improvementTips)
            ) : (
              <div className="rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-400">
                暂无待改进点
              </div>
            )}
          </section>
        </div>
      ) : null}
    </Card>
  );
}

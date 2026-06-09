import { useMemo, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { ChevronDown, ChevronUp } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import type { QaReview } from "@/components/interview/report/types";

type InterviewReferenceAnswerCardProps = {
  qaReviews: QaReview[];
  isLoading: boolean;
};

type ReferenceItem = {
  key: string;
  label: string;
  question: string;
  referenceAnswer: string;
};

const buildReferenceItems = (qaReviews: QaReview[]): ReferenceItem[] => {
  return qaReviews
    .filter(
      (item) =>
        item.referenceAnswer && item.referenceAnswer.trim().length > 0,
    )
    .map((item, index) => {
      const label = item.isFollowUp
        ? `追问 ${item.questionNumber || index + 1}`
        : `主问题 ${item.questionNumber || index + 1}`;

      return {
        key: `${item.questionNumber || "qa"}-${index}`,
        label,
        question: item.question,
        referenceAnswer: item.referenceAnswer!.trim(),
      };
    });
};

export default function InterviewReferenceAnswerCard({
  qaReviews,
  isLoading,
}: InterviewReferenceAnswerCardProps) {
  const [expandedMap, setExpandedMap] = useState<Record<string, boolean>>({});

  const referenceItems = useMemo(
    () => buildReferenceItems(qaReviews),
    [qaReviews],
  );

  const toggleExpand = (key: string) => {
    setExpandedMap((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  return (
    <Card className="min-w-0 border-slate-100 p-6">
      <p className="text-sm font-medium text-slate-900">参考答案</p>

      {isLoading ? (
        <div className="mt-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          正在加载参考答案...
        </div>
      ) : referenceItems.length === 0 ? (
        <div className="mt-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          暂无参考答案
        </div>
      ) : (
        <div className="mt-4 space-y-3">
          {referenceItems.map((item, index) => {
            const isExpanded = Boolean(expandedMap[item.key]);

            return (
              <motion.div
                key={item.key}
                className="min-w-0 rounded-lg border border-slate-100 p-4"
                initial={{ opacity: 0, y: 12, filter: "blur(4px)" }}
                animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
                transition={{
                  duration: 0.28,
                  delay: index * 0.06,
                  ease: [0.22, 1, 0.36, 1],
                }}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 space-y-1">
                    <span className="text-[11px] text-slate-500">
                      {item.label}
                    </span>
                    <p className="line-clamp-2 break-words text-sm font-medium leading-5 text-slate-900">
                      {item.question}
                    </p>
                  </div>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className={cn(
                      "shrink-0 rounded-full h-7 px-3 text-xs text-blue-700 hover:bg-blue-50 hover:text-blue-800",
                      isExpanded &&
                        "border border-blue-200 bg-blue-50 text-blue-800",
                    )}
                    onClick={() => toggleExpand(item.key)}
                  >
                    {isExpanded ? "收起" : "查看参考答案"}
                    {isExpanded ? (
                      <ChevronUp className="ml-1 h-3.5 w-3.5" />
                    ) : (
                      <ChevronDown className="ml-1 h-3.5 w-3.5" />
                    )}
                  </Button>
                </div>

                <AnimatePresence initial={false}>
                  {isExpanded && (
                    <motion.div
                      key={`${item.key}-ref`}
                      className="overflow-hidden"
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: "auto", opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }}
                    >
                      <div className="mt-3 break-words rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-xs leading-6 text-blue-800">
                        {item.referenceAnswer}
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            );
          })}
        </div>
      )}
    </Card>
  );
}

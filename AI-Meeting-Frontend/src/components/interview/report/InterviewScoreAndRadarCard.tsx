import { motion } from "framer-motion";
import { Card } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import InterviewRadarChart from "@/components/interview/report/InterviewRadarChart";
import type { RadarPoint } from "@/components/interview/report/types";
import type { DimensionScores } from "@/hooks/interview/report/interviewReportData.shared";
import { cn } from "@/lib/utils";

type InterviewScoreAndRadarCardProps = {
  resumeScore: number | null;
  interviewScore: number | null;
  compositeScore: number | null;
  isCompositeEstimated: boolean;
  radarPoints: RadarPoint[];
  dimensionScores: DimensionScores;
};

const DIMENSION_CONFIG = [
  { key: "contentScore" as const, label: "内容质量", max: 30, color: "bg-blue-500" },
  { key: "logicScore" as const, label: "逻辑结构", max: 15, color: "bg-green-500" },
  { key: "professionalScore" as const, label: "专业匹配", max: 15, color: "bg-purple-500" },
  { key: "expressionScore" as const, label: "语言表达", max: 15, color: "bg-amber-500" },
  { key: "adaptabilityScore" as const, label: "临场应变", max: 10, color: "bg-red-500" },
  { key: "timeControlScore" as const, label: "时间控制", max: 5, color: "bg-cyan-500" },
  { key: "etiquetteScore" as const, label: "礼仪仪态", max: 10, color: "bg-pink-500" },
] as const;

const formatScore = (value: number | null) =>
  value === null ? "--" : String(value);

export default function InterviewScoreAndRadarCard({
  resumeScore,
  interviewScore,
  compositeScore,
  isCompositeEstimated,
  radarPoints,
  dimensionScores,
}: InterviewScoreAndRadarCardProps) {
  const hasAnyDimensionScore = DIMENSION_CONFIG.some(
    (dim) => dimensionScores[dim.key] !== null,
  );
  const scoreCards = [
    { label: "简历得分", value: resumeScore },
    { label: "回答得分", value: interviewScore },
    { label: "综合评分", value: compositeScore },
  ];

  return (
    <Card className="p-6 border-slate-100">
      <div className="grid md:grid-cols-3 gap-4">
        {scoreCards.map((item, index) => (
          <motion.div
            key={item.label}
            className="rounded-lg bg-slate-50 p-4"
            initial={{ opacity: 0, y: 14, filter: "blur(4px)" }}
            animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            transition={{
              duration: 0.28,
              delay: index * 0.06,
              ease: [0.22, 1, 0.36, 1],
            }}
          >
            <p className="text-xs text-slate-500">{item.label}</p>
            <p className="text-2xl font-semibold text-slate-900">
              {formatScore(item.value)}
            </p>
            {item.label === "综合评分" && isCompositeEstimated ? (
              <p className="mt-1 text-[10px] text-slate-400">估算值</p>
            ) : null}
          </motion.div>
        ))}
      </div>
      <Separator className="my-6" />
      <div className="grid md:grid-cols-[0.9fr_1.1fr] gap-6 items-center">
        <div>
          <p className="text-sm font-medium text-slate-900">能力雷达图</p>
          <p className="text-xs text-slate-500 mt-1">
            基于本次会话实际数据计算，若后端未返回则显示为空。
          </p>
          <div className="mt-4 space-y-3">
            {radarPoints.length > 0 ? (
              radarPoints.map((item, index) => (
                <motion.div
                  key={item.label}
                  className="space-y-1"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{
                    duration: 0.24,
                    delay: index * 0.05,
                    ease: [0.22, 1, 0.36, 1],
                  }}
                >
                  <div className="flex items-center justify-between text-xs text-slate-500">
                    <span>{item.label}</span>
                    <span>{item.value}</span>
                  </div>
                  <div className="h-2 rounded-full bg-slate-100">
                    <motion.div
                      className="h-2 rounded-full bg-indigo-500"
                      initial={{ width: 0 }}
                      animate={{ width: `${item.value}%` }}
                      transition={{
                        duration: 0.45,
                        delay: index * 0.05 + 0.08,
                        ease: "easeOut",
                      }}
                    />
                  </div>
                </motion.div>
              ))
            ) : (
              <div className="rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
                暂无雷达维度数据
              </div>
            )}
          </div>
        </div>
        <motion.div
          initial={{ opacity: 0, scale: 0.96, filter: "blur(4px)" }}
          animate={{ opacity: 1, scale: 1, filter: "blur(0px)" }}
          transition={{ duration: 0.32, delay: 0.12, ease: [0.22, 1, 0.36, 1] }}
        >
          <InterviewRadarChart points={radarPoints} />
        </motion.div>
      </div>
      {hasAnyDimensionScore && (
        <>
          <Separator className="my-6" />
          <div>
            <p className="text-sm font-medium text-slate-900">七维评分详情</p>
            <p className="text-xs text-slate-500 mt-1">
              各维度得分与满分对比，帮助定位优势与薄弱环节。
            </p>
            <div className="mt-4 space-y-3">
              {DIMENSION_CONFIG.map((dim, index) => {
                const score = dimensionScores[dim.key];
                const percentage =
                  score !== null ? Math.min(100, (score / dim.max) * 100) : 0;

                return (
                  <motion.div
                    key={dim.key}
                    className="space-y-1"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{
                      duration: 0.24,
                      delay: index * 0.05,
                      ease: [0.22, 1, 0.36, 1],
                    }}
                  >
                    <div className="flex items-center justify-between text-xs text-slate-500">
                      <span>{dim.label}</span>
                      <span>
                        {score !== null ? score : "--"}{" "}
                        <span className="text-slate-400">/ {dim.max}</span>
                      </span>
                    </div>
                    <div className="h-2 rounded-full bg-slate-100">
                      <motion.div
                        className={cn("h-2 rounded-full", dim.color)}
                        initial={{ width: 0 }}
                        animate={{ width: `${percentage}%` }}
                        transition={{
                          duration: 0.45,
                          delay: index * 0.05 + 0.08,
                          ease: "easeOut",
                        }}
                      />
                    </div>
                  </motion.div>
                );
              })}
            </div>
          </div>
        </>
      )}
    </Card>
  );
}

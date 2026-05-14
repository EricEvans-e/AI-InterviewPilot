import { useMemo } from "react";
import { motion } from "framer-motion";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import type { InterviewRecordResult } from "@/services/interviewService";

type InterviewEtiquetteCardProps = {
  record: InterviewRecordResult | null;
  isLoading: boolean;
};

type DemeanorDetails = {
  panicLevel?: number | null;
  seriousnessLevel?: number | null;
  emoticonHandling?: number | null;
  compositeScore?: number | null;
};

type EtiquetteData = {
  etiquetteScore: number | null;
  demeanorScore: number | null;
  demeanorDetails: DemeanorDetails | null;
};

const toNumber = (value: unknown): number | null => {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim() !== "") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
};

const parseJsonRecord = (value: unknown): Record<string, unknown> | null => {
  if (typeof value !== "string" || value.trim() === "") return null;
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === "object" && !Array.isArray(parsed)
      ? (parsed as Record<string, unknown>)
      : null;
  } catch {
    return null;
  }
};

const extractEtiquetteData = (record: InterviewRecordResult | null): EtiquetteData => {
  if (!record) {
    return { etiquetteScore: null, demeanorScore: null, demeanorDetails: null };
  }

  const radarObj =
    record.radarChart && typeof record.radarChart === "object"
      ? (record.radarChart as Record<string, unknown>)
      : null;

  const etiquetteScore =
    toNumber(radarObj?.etiquetteScore) ?? toNumber((record as unknown as Record<string, unknown>)?.etiquetteScore);

  const snapshot = parseJsonRecord(record.sessionSnapshotJson);
  const demeanorScore =
    toNumber(snapshot?.demeanorScore) ?? toNumber(radarObj?.demeanorEvaluation);

  const rawDetails = snapshot?.demeanorDetails;
  let demeanorDetails: DemeanorDetails | null = null;
  if (rawDetails && typeof rawDetails === "object" && !Array.isArray(rawDetails)) {
    const detailsObj = rawDetails as Record<string, unknown>;
    demeanorDetails = {
      panicLevel: toNumber(detailsObj.panicLevel),
      seriousnessLevel: toNumber(detailsObj.seriousnessLevel),
      emoticonHandling: toNumber(detailsObj.emoticonHandling),
      compositeScore: toNumber(detailsObj.compositeScore),
    };
  }

  return { etiquetteScore, demeanorScore, demeanorDetails };
};

const DEMEANOR_METRICS = [
  { key: "panicLevel" as const, label: "慌乱度", description: "表现沉稳程度，越低越好" },
  { key: "seriousnessLevel" as const, label: "严肃程度", description: "面试态度认真度" },
  { key: "emoticonHandling" as const, label: "表情管理", description: "面部表情自然度" },
] as const;

const formatScore = (value: number | null | undefined) =>
  value === null || value === undefined ? "--" : String(value);

export default function InterviewEtiquetteCard({
  record,
  isLoading,
}: InterviewEtiquetteCardProps) {
  const data = useMemo(() => extractEtiquetteData(record), [record]);

  const hasAnyData =
    data.etiquetteScore !== null ||
    data.demeanorScore !== null ||
    data.demeanorDetails !== null;

  return (
    <Card className="border-slate-100 p-6">
      <p className="text-sm font-medium text-slate-900">礼仪仪态评估</p>

      {isLoading ? (
        <div className="mt-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          正在加载礼仪评估数据...
        </div>
      ) : !hasAnyData ? (
        <div className="mt-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          暂无礼仪评估数据
        </div>
      ) : (
        <div className="mt-4 space-y-4">
          {/* Top-level scores */}
          <div className="grid grid-cols-2 gap-3">
            {data.etiquetteScore !== null && (
              <motion.div
                className="rounded-lg bg-pink-50 p-3"
                initial={{ opacity: 0, y: 10, filter: "blur(3px)" }}
                animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
                transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
              >
                <p className="text-xs text-pink-600">礼仪仪态得分</p>
                <p className="mt-1 text-2xl font-semibold text-pink-700">
                  {formatScore(data.etiquetteScore)}
                </p>
                <p className="text-[10px] text-pink-400">/ 10</p>
              </motion.div>
            )}
            {data.demeanorScore !== null && (
              <motion.div
                className="rounded-lg bg-indigo-50 p-3"
                initial={{ opacity: 0, y: 10, filter: "blur(3px)" }}
                animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
                transition={{ duration: 0.28, delay: 0.06, ease: [0.22, 1, 0.36, 1] }}
              >
                <p className="text-xs text-indigo-600">神态综合得分</p>
                <p className="mt-1 text-2xl font-semibold text-indigo-700">
                  {formatScore(data.demeanorScore)}
                </p>
                <p className="text-[10px] text-indigo-400">/ 100</p>
              </motion.div>
            )}
          </div>

          {/* Detailed demeanor metrics */}
          {data.demeanorDetails && (
            <div className="space-y-3">
              <p className="text-xs font-medium uppercase tracking-[0.18em] text-slate-400">
                神态分析详情
              </p>
              {DEMEANOR_METRICS.map((metric, index) => {
                const value = data.demeanorDetails?.[metric.key] ?? null;
                if (value === null) return null;

                return (
                  <motion.div
                    key={metric.key}
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
                      <span>{metric.label}</span>
                      <span>{formatScore(value)}</span>
                    </div>
                    <div className="h-2 rounded-full bg-slate-100">
                      <motion.div
                        className={cn(
                          "h-2 rounded-full",
                          metric.key === "panicLevel"
                            ? "bg-amber-500"
                            : metric.key === "seriousnessLevel"
                              ? "bg-blue-500"
                              : "bg-emerald-500",
                        )}
                        initial={{ width: 0 }}
                        animate={{ width: `${Math.min(100, value)}%` }}
                        transition={{
                          duration: 0.45,
                          delay: index * 0.05 + 0.08,
                          ease: "easeOut",
                        }}
                      />
                    </div>
                    <p className="text-[10px] text-slate-400">
                      {metric.description}
                    </p>
                  </motion.div>
                );
              })}

              {data.demeanorDetails.compositeScore !== null && (
                <motion.div
                  className="mt-2 rounded-lg bg-slate-50 p-3"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{
                    duration: 0.24,
                    delay: 0.2,
                    ease: [0.22, 1, 0.36, 1],
                  }}
                >
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-medium text-slate-700">
                      神态综合评分
                    </span>
                    <span className="font-semibold text-slate-900">
                      {formatScore(data.demeanorDetails.compositeScore)}
                    </span>
                  </div>
                </motion.div>
              )}
            </div>
          )}
        </div>
      )}
    </Card>
  );
}

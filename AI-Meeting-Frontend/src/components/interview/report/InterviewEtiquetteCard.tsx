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
  evaluationBasis?: string[];
  improvementSuggestions?: string[];
};

type EtiquetteData = {
  etiquetteScore: number | null;
  demeanorPerformanceScore: number | null;
  demeanorDetails: DemeanorDetails | null;
  evaluationBasis: string[];
  improvementSuggestions: string[];
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

const toStringArray = (value: unknown): string[] => {
  if (Array.isArray(value)) {
    return value
      .map((item) => (typeof item === "string" ? item.trim() : ""))
      .filter(Boolean);
  }

  if (typeof value === "string") {
    return value
      .split(/\n|；|;/u)
      .map((item) => item.trim())
      .filter(Boolean);
  }

  if (value && typeof value === "object") {
    return Object.values(value as Record<string, unknown>)
      .map((item) => (typeof item === "string" ? item.trim() : ""))
      .filter(Boolean);
  }

  return [];
};

const pickStringArray = (
  source: Record<string, unknown> | null | undefined,
  keys: string[],
) => {
  if (!source) return [];

  for (const key of keys) {
    const values = toStringArray(source[key]);
    if (values.length > 0) {
      return values;
    }
  }

  return [];
};

const getScoreBand = (score: number | null | undefined) => {
  if (score === null || score === undefined) return "unknown";
  if (score >= 80) return "strong";
  if (score >= 60) return "steady";
  return "weak";
};

const buildFallbackBasis = (
  etiquetteScore: number | null,
  demeanorPerformanceScore: number | null,
  details: DemeanorDetails | null,
) => {
  const basis: string[] = [];

  if (etiquetteScore !== null || demeanorPerformanceScore !== null) {
    const scoreSummary = [
      etiquetteScore !== null ? `礼仪仪态得分为 ${etiquetteScore}/100` : null,
      demeanorPerformanceScore !== null
        ? `神态表现得分为 ${demeanorPerformanceScore}/100`
        : null,
    ].filter(Boolean);

    basis.push(
      `${scoreSummary.join("，")}，综合反映候选人的镜头礼仪、专注状态和表达稳定性。`,
    );
  }

  if (details?.panicLevel !== null && details?.panicLevel !== undefined) {
    const panicText =
      details.panicLevel >= 65
        ? "紧张或慌乱迹象较明显"
        : details.panicLevel >= 40
          ? "存在一定紧张波动"
          : "紧张控制较好";
    basis.push(`慌乱度为 ${details.panicLevel}/100，${panicText}。`);
  }

  if (
    details?.seriousnessLevel !== null &&
    details?.seriousnessLevel !== undefined
  ) {
    const seriousnessText =
      details.seriousnessLevel >= 75
        ? "面试态度较专注认真"
        : details.seriousnessLevel >= 60
          ? "专注度基本稳定"
          : "专注和正式感仍有提升空间";
    basis.push(
      `严肃程度为 ${details.seriousnessLevel}/100，${seriousnessText}。`,
    );
  }

  if (
    details?.emoticonHandling !== null &&
    details?.emoticonHandling !== undefined
  ) {
    const expressionText =
      details.emoticonHandling >= 75
        ? "表情管理自然稳定"
        : details.emoticonHandling >= 60
          ? "表情控制基本可接受"
          : "表情自然度和稳定性偏弱";
    basis.push(
      `表情管理为 ${details.emoticonHandling}/100，${expressionText}。`,
    );
  }

  return basis.slice(0, 4);
};

const buildFallbackSuggestions = (
  etiquetteScore: number | null,
  demeanorPerformanceScore: number | null,
  details: DemeanorDetails | null,
) => {
  const suggestions: string[] = [];

  if (
    details?.panicLevel !== null &&
    details?.panicLevel !== undefined &&
    details.panicLevel >= 60
  ) {
    suggestions.push(
      "回答前先稳定呼吸，停顿一秒再开口，降低紧张造成的表情和语速波动。",
    );
  }

  if (
    details?.seriousnessLevel !== null &&
    details?.seriousnessLevel !== undefined &&
    details.seriousnessLevel < 70
  ) {
    suggestions.push(
      "保持正视镜头和端正坐姿，回答时减少低头、偏头和视线游离。",
    );
  }

  if (
    details?.emoticonHandling !== null &&
    details?.emoticonHandling !== undefined &&
    details.emoticonHandling < 70
  ) {
    suggestions.push(
      "练习用自然微表情承接问题，避免长时间僵硬、皱眉或表情过度紧绷。",
    );
  }

  const overallBand = getScoreBand(demeanorPerformanceScore ?? etiquetteScore);
  if (suggestions.length === 0) {
    suggestions.push(
      overallBand === "strong"
        ? "当前礼仪和神态表现较稳定，后续重点保持镜头交流、坐姿稳定和表达节奏即可。"
        : "建议用 2-3 分钟模拟问答训练镜头感，重点观察坐姿、眼神和表情是否稳定。",
    );
  }

  return suggestions.slice(0, 3);
};

const extractEtiquetteData = (
  record: InterviewRecordResult | null,
): EtiquetteData => {
  if (!record) {
    return {
      etiquetteScore: null,
      demeanorPerformanceScore: null,
      demeanorDetails: null,
      evaluationBasis: [],
      improvementSuggestions: [],
    };
  }

  const radarObj =
    record.radarChart && typeof record.radarChart === "object"
      ? (record.radarChart as Record<string, unknown>)
      : null;

  const etiquetteScore =
    toNumber(radarObj?.etiquetteScore) ??
    toNumber((record as unknown as Record<string, unknown>)?.etiquetteScore);

  const snapshot = parseJsonRecord(record.sessionSnapshotJson);

  const rawDetails = snapshot?.demeanorDetails;
  let demeanorDetails: DemeanorDetails | null = null;
  if (
    rawDetails &&
    typeof rawDetails === "object" &&
    !Array.isArray(rawDetails)
  ) {
    const detailsObj = rawDetails as Record<string, unknown>;
    demeanorDetails = {
      panicLevel: toNumber(detailsObj.panicLevel),
      seriousnessLevel: toNumber(detailsObj.seriousnessLevel),
      emoticonHandling: toNumber(detailsObj.emoticonHandling),
      compositeScore: toNumber(detailsObj.compositeScore),
      evaluationBasis: pickStringArray(detailsObj, [
        "evaluationBasis",
        "assessmentBasis",
        "analysisBasis",
        "basis",
        "evidence",
        "reasons",
      ]),
      improvementSuggestions: pickStringArray(detailsObj, [
        "improvementSuggestions",
        "improvementTips",
        "suggestions",
        "recommendations",
        "advice",
      ]),
    };
  }

  const demeanorPerformanceScore =
    toNumber(demeanorDetails?.compositeScore) ??
    toNumber(snapshot?.demeanorScore) ??
    toNumber(radarObj?.demeanorEvaluation);

  const snapshotBasis = pickStringArray(snapshot, [
    "demeanorEvaluationBasis",
    "etiquetteEvaluationBasis",
    "evaluationBasis",
    "assessmentBasis",
  ]);
  const snapshotSuggestions = pickStringArray(snapshot, [
    "demeanorImprovementSuggestions",
    "etiquetteImprovementSuggestions",
    "improvementSuggestions",
    "improvementTips",
  ]);

  const aiEvaluationBasis =
    demeanorDetails?.evaluationBasis &&
    demeanorDetails.evaluationBasis.length > 0
      ? demeanorDetails.evaluationBasis
      : snapshotBasis;
  const aiImprovementSuggestions =
    demeanorDetails?.improvementSuggestions &&
    demeanorDetails.improvementSuggestions.length > 0
      ? demeanorDetails.improvementSuggestions
      : snapshotSuggestions;

  return {
    etiquetteScore,
    demeanorPerformanceScore,
    demeanorDetails,
    evaluationBasis:
      aiEvaluationBasis.length > 0
        ? aiEvaluationBasis
        : buildFallbackBasis(
            etiquetteScore,
            demeanorPerformanceScore,
            demeanorDetails,
          ),
    improvementSuggestions:
      aiImprovementSuggestions.length > 0
        ? aiImprovementSuggestions
        : buildFallbackSuggestions(
            etiquetteScore,
            demeanorPerformanceScore,
            demeanorDetails,
          ),
  };
};

const DEMEANOR_METRICS = [
  {
    key: "panicLevel" as const,
    label: "慌乱度",
    description: "可见紧张程度，分数越低越好",
  },
  {
    key: "seriousnessLevel" as const,
    label: "严肃程度",
    description: "面试态度的专注和认真程度",
  },
  {
    key: "emoticonHandling" as const,
    label: "表情管理",
    description: "面部表情的自然和稳定程度",
  },
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
    data.demeanorPerformanceScore !== null ||
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
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {data.etiquetteScore !== null && (
              <motion.div
                className="min-w-0 rounded-lg bg-pink-50 p-3"
                initial={{ opacity: 0, y: 10, filter: "blur(3px)" }}
                animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
                transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
              >
                <p className="text-xs text-pink-600">礼仪仪态得分</p>
                <p className="mt-1 text-2xl font-semibold text-pink-700">
                  {formatScore(data.etiquetteScore)}
                </p>
                <p className="text-[10px] text-pink-400">/ 100</p>
              </motion.div>
            )}
            {data.demeanorPerformanceScore !== null && (
              <motion.div
                className="min-w-0 rounded-lg bg-indigo-50 p-3"
                initial={{ opacity: 0, y: 10, filter: "blur(3px)" }}
                animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
                transition={{
                  duration: 0.28,
                  delay: 0.06,
                  ease: [0.22, 1, 0.36, 1],
                }}
              >
                <p className="text-xs text-indigo-600">神态表现得分</p>
                <p className="mt-1 text-2xl font-semibold text-indigo-700">
                  {formatScore(data.demeanorPerformanceScore)}
                </p>
                <p className="text-[10px] text-indigo-400">/ 100</p>
              </motion.div>
            )}
          </div>

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
                    <div className="flex items-center justify-between gap-3 text-xs text-slate-500">
                      <span className="min-w-0 break-words">
                        {metric.label}
                      </span>
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
                      神态表现评分
                    </span>
                    <span className="font-semibold text-slate-900">
                      {formatScore(data.demeanorDetails.compositeScore)}
                    </span>
                  </div>
                </motion.div>
              )}
            </div>
          )}

          {(data.evaluationBasis.length > 0 ||
            data.improvementSuggestions.length > 0) && (
            <div className="grid gap-3">
              {data.evaluationBasis.length > 0 && (
                <div className="rounded-lg border border-slate-100 bg-slate-50 p-3">
                  <p className="text-xs font-medium text-slate-700">评估依据</p>
                  <ul className="mt-2 space-y-1.5 text-xs leading-6 text-slate-500">
                    {data.evaluationBasis.map((item) => (
                      <li key={item} className="break-words">
                        {item}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {data.improvementSuggestions.length > 0 && (
                <div className="rounded-lg border border-indigo-100 bg-indigo-50 p-3">
                  <p className="text-xs font-medium text-indigo-700">
                    改进建议
                  </p>
                  <ul className="mt-2 space-y-1.5 text-xs leading-6 text-indigo-700">
                    {data.improvementSuggestions.map((item) => (
                      <li key={item} className="break-words">
                        {item}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </Card>
  );
}

import { useEffect, useMemo, useState } from "react";
import { Bot, ChevronDown, ChevronUp, Save, Wand2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import {
  ABILITY_TAG_OPTIONS,
  DIFFICULTY_OPTIONS,
  QUESTION_TYPE_OPTIONS,
  type CollegeRespDTO,
  type MajorRespDTO,
  type QuestionRespDTO,
} from "@/services/questionBankService";
import {
  teacherService,
  type AiExpandParams,
  type AiPropertiesDTO,
  type QuestionCreateDTO,
} from "@/services/teacherService";

interface AiExpandDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  selectedQuestionIds: number[];
  selectedQuestions: QuestionRespDTO[];
  onBatchSave: (questions: QuestionCreateDTO[]) => void;
  isSaving: boolean;
}

function normalizeCountInput(value: string): number {
  if (!value.trim()) {
    return 1;
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return 1;
  }
  return Math.max(1, Math.min(20, Math.trunc(parsed)));
}

function getDifficultyLabel(value?: string) {
  if (!value) return "-";
  return DIFFICULTY_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

export default function AiExpandDialog({
  open,
  onOpenChange,
  selectedQuestionIds,
  selectedQuestions,
  onBatchSave,
  isSaving,
}: AiExpandDialogProps) {
  const [collegeId, setCollegeId] = useState<number | "">("");
  const [majorId, setMajorId] = useState<number | "">("");
  const [questionType, setQuestionType] = useState("");
  const [abilityTag, setAbilityTag] = useState("");
  const [countInput, setCountInput] = useState("5");
  const [difficulty, setDifficulty] = useState("");
  const [generateReferenceAnswer, setGenerateReferenceAnswer] = useState(true);
  const [generateFollowUp, setGenerateFollowUp] = useState(false);
  const [generateScoringRule, setGenerateScoringRule] = useState(false);

  const [colleges, setColleges] = useState<CollegeRespDTO[]>([]);
  const [majors, setMajors] = useState<MajorRespDTO[]>([]);
  const [aiModels, setAiModels] = useState<AiPropertiesDTO[]>([]);
  const [selectedModelId, setSelectedModelId] = useState<number | "">("");

  const [generatedQuestions, setGeneratedQuestions] = useState<QuestionCreateDTO[]>([]);
  const [expandedQuestionIndexes, setExpandedQuestionIndexes] = useState<number[]>([]);
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedTitles = useMemo(
    () => selectedQuestions.map((question) => question.title).filter(Boolean),
    [selectedQuestions],
  );

  useEffect(() => {
    if (!open) return;

    teacherService.listColleges().then(setColleges).catch(() => setColleges([]));
    teacherService.getEnabledAiProperties().then(setAiModels).catch(() => setAiModels([]));
    setGeneratedQuestions([]);
    setExpandedQuestionIndexes([]);
    setError(null);
    setSelectedModelId("");
  }, [open]);

  useEffect(() => {
    if (!collegeId) {
      setMajors([]);
      return;
    }

    let cancelled = false;
    teacherService
      .listMajors(collegeId as number)
      .then((data) => {
        if (!cancelled) setMajors(data);
      })
      .catch(() => {
        if (!cancelled) setMajors([]);
      });

    return () => {
      cancelled = true;
    };
  }, [collegeId]);

  const handleCollegeChange = (value: string) => {
    const nextCollegeId = value ? Number(value) : "";
    setCollegeId(nextCollegeId);
    setMajorId("");
    setMajors([]);
  };

  const handleGenerate = async () => {
    if (selectedQuestionIds.length === 0 || !questionType) {
      return;
    }

    const count = normalizeCountInput(countInput);
    setCountInput(String(count));
    setIsGenerating(true);
    setError(null);
    setGeneratedQuestions([]);
    setExpandedQuestionIndexes([]);

    try {
      const params: AiExpandParams = {
        referenceQuestionIds: selectedQuestionIds,
        collegeId: collegeId ? (collegeId as number) : undefined,
        majorId: majorId ? (majorId as number) : undefined,
        questionType: questionType || undefined,
        abilityTag: abilityTag || undefined,
        count,
        difficulty: difficulty || undefined,
        generateReferenceAnswer,
        generateFollowUp,
        generateScoringRule,
        aiPropertiesId: selectedModelId ? (selectedModelId as number) : undefined,
      };

      const result = await teacherService.aiExpandQuestions(params);
      setGeneratedQuestions(Array.isArray(result) ? result : (result.questions ?? []));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "AI 拓题失败，请重试");
    } finally {
      setIsGenerating(false);
    }
  };

  const handleRemoveQuestion = (index: number) => {
    setGeneratedQuestions((previous) => previous.filter((_, currentIndex) => currentIndex !== index));
    setExpandedQuestionIndexes((previous) =>
      previous
        .filter((currentIndex) => currentIndex !== index)
        .map((currentIndex) => (currentIndex > index ? currentIndex - 1 : currentIndex)),
    );
  };

  const handleTogglePreview = (index: number) => {
    setExpandedQuestionIndexes((previous) =>
      previous.includes(index)
        ? previous.filter((currentIndex) => currentIndex !== index)
        : [...previous, index],
    );
  };

  const handleSaveAll = () => {
    if (generatedQuestions.length === 0) {
      return;
    }
    onBatchSave(
      generatedQuestions.map((question) => ({
        ...question,
        isAiGenerated: true,
        status: "pending_review",
      })),
    );
  };

  const getQuestionTypeLabel = (value?: string) => {
    if (!value) return "-";
    return QUESTION_TYPE_OPTIONS.find((option) => option.value === value)?.label ?? value;
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[90vh] max-w-4xl flex-col overflow-hidden">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Wand2 className="h-5 w-5 text-violet-600" />
            AI 拓题
          </DialogTitle>
          <DialogDescription>
            基于已选参考题生成一批待审核新题，院校、专业、题型等信息由当前配置决定。
          </DialogDescription>
        </DialogHeader>

        <div className="min-h-0 flex-1 overflow-y-auto pr-2">
          <div className="space-y-4 pr-2">
            <div className="rounded-md border border-slate-200 bg-slate-50 px-4 py-3">
              <div className="flex items-center gap-2 text-sm font-medium text-slate-700">
                <Bot className="h-4 w-4 text-violet-600" />
                已选参考题 {selectedQuestionIds.length} 道
              </div>
              <p className="mt-1 text-xs text-slate-500">
                这些题目只作为风格和方向参考，不会自动继承院校、专业、题型等元数据。
              </p>
              <ScrollArea className="mt-3 max-h-28">
                <div className="space-y-2 pr-3">
                  {selectedTitles.map((title, index) => (
                    <div
                      key={`${title}-${index}`}
                      className="rounded border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700"
                    >
                      {index + 1}. {title}
                    </div>
                  ))}
                </div>
              </ScrollArea>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <Label>院校</Label>
                <select
                  value={collegeId}
                  onChange={(event) => handleCollegeChange(event.target.value)}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <option value="">不限定</option>
                  {colleges.map((college) => (
                    <option key={college.id} value={college.id}>
                      {college.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-1.5">
                <Label>专业</Label>
                <select
                  value={majorId}
                  onChange={(event) => setMajorId(event.target.value ? Number(event.target.value) : "")}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  disabled={!collegeId}
                >
                  <option value="">不限定</option>
                  {majors.map((major) => (
                    <option key={major.id} value={major.id}>
                      {major.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-1.5">
                <Label>AI 模型</Label>
                <select
                  value={selectedModelId}
                  onChange={(event) => setSelectedModelId(event.target.value ? Number(event.target.value) : "")}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <option value="">系统默认</option>
                  {aiModels.map((model) => (
                    <option key={model.id} value={model.id}>
                      {model.aiName} ({model.modelName})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-1.5">
                <Label>
                  题型 <span className="text-red-500">*</span>
                </Label>
                <select
                  value={questionType}
                  onChange={(event) => setQuestionType(event.target.value)}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <option value="">请选择题型</option>
                  {QUESTION_TYPE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-1.5">
                <Label>能力点</Label>
                <select
                  value={abilityTag}
                  onChange={(event) => setAbilityTag(event.target.value)}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <option value="">不限定</option>
                  {ABILITY_TAG_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-1.5">
                <Label>难度</Label>
                <select
                  value={difficulty}
                  onChange={(event) => setDifficulty(event.target.value)}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <option value="">不限定</option>
                  {DIFFICULTY_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="grid grid-cols-4 gap-4">
              <div className="space-y-1.5">
                <Label>生成数量 (1-20)</Label>
                <Input
                  type="number"
                  value={countInput}
                  min={1}
                  max={20}
                  onChange={(event) => setCountInput(event.target.value)}
                  onBlur={() => setCountInput(String(normalizeCountInput(countInput)))}
                />
              </div>
              <label className="flex items-end gap-2 pb-2 text-sm">
                <input
                  type="checkbox"
                  checked={generateReferenceAnswer}
                  onChange={(event) => setGenerateReferenceAnswer(event.target.checked)}
                  className="h-4 w-4 rounded border-slate-300"
                />
                生成参考答案
              </label>
              <label className="flex items-end gap-2 pb-2 text-sm">
                <input
                  type="checkbox"
                  checked={generateFollowUp}
                  onChange={(event) => setGenerateFollowUp(event.target.checked)}
                  className="h-4 w-4 rounded border-slate-300"
                />
                生成追问
              </label>
              <label className="flex items-end gap-2 pb-2 text-sm">
                <input
                  type="checkbox"
                  checked={generateScoringRule}
                  onChange={(event) => setGenerateScoringRule(event.target.checked)}
                  className="h-4 w-4 rounded border-slate-300"
                />
                生成评分标准
              </label>
            </div>

            <Button
              onClick={handleGenerate}
              disabled={isGenerating || selectedQuestionIds.length === 0 || !questionType}
              className="w-full"
            >
              {isGenerating ? "AI 拓题中..." : "开始拓题"}
            </Button>

            {error ? <p className="text-sm text-red-500">{error}</p> : null}

            {generatedQuestions.length > 0 ? (
              <>
                <Separator />
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-medium text-slate-800">
                      生成结果 ({generatedQuestions.length} 道)
                    </h3>
                  </div>
                  <div className="space-y-3">
                    {generatedQuestions.map((question, index) => (
                      <div
                        key={`${question.title}-${index}`}
                        className="rounded-md border border-slate-200 bg-slate-50"
                      >
                        <div className="flex items-start justify-between gap-2 p-3">
                          <button
                            type="button"
                            className="flex flex-1 items-start justify-between gap-3 text-left"
                            aria-expanded={expandedQuestionIndexes.includes(index)}
                            onClick={() => handleTogglePreview(index)}
                          >
                            <div className="flex-1">
                              <p className="text-sm font-medium text-slate-800">{question.title}</p>
                              <p className="mt-1 text-xs text-slate-500">
                                {getQuestionTypeLabel(question.questionType)}
                                {question.difficulty ? ` | ${getDifficultyLabel(question.difficulty)}` : ""}
                                {question.abilityTag ? ` | ${question.abilityTag}` : ""}
                              </p>
                              {question.content ? (
                                <p
                                  className={`mt-1.5 text-xs text-slate-600 ${
                                    expandedQuestionIndexes.includes(index) ? "" : "line-clamp-2"
                                  }`}
                                >
                                  {question.content}
                                </p>
                              ) : null}
                            </div>
                            {expandedQuestionIndexes.includes(index) ? (
                              <ChevronUp className="mt-0.5 h-4 w-4 shrink-0 text-slate-400" />
                            ) : (
                              <ChevronDown className="mt-0.5 h-4 w-4 shrink-0 text-slate-400" />
                            )}
                          </button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-6 w-6 shrink-0 text-slate-400 hover:text-red-500"
                            onClick={(event) => {
                              event.stopPropagation();
                              handleRemoveQuestion(index);
                            }}
                          >
                            <X className="h-3.5 w-3.5" />
                          </Button>
                        </div>
                        {expandedQuestionIndexes.includes(index) ? (
                          <div className="border-t border-slate-200 px-3 py-3 text-xs text-slate-600">
                            <div className="grid gap-3 md:grid-cols-2">
                              <div>
                                <p className="font-medium text-slate-700">题型</p>
                                <p className="mt-1">{getQuestionTypeLabel(question.questionType)}</p>
                              </div>
                              <div>
                                <p className="font-medium text-slate-700">难度</p>
                                <p className="mt-1">{getDifficultyLabel(question.difficulty)}</p>
                              </div>
                              <div>
                                <p className="font-medium text-slate-700">能力点</p>
                                <p className="mt-1">{question.abilityTag || "-"}</p>
                              </div>
                              <div>
                                <p className="font-medium text-slate-700">建议作答时长</p>
                                <p className="mt-1">
                                  {question.answerTimeSeconds ? `${question.answerTimeSeconds} 秒` : "-"}
                                </p>
                              </div>
                            </div>
                            {question.referenceAnswer ? (
                              <div className="mt-3">
                                <p className="font-medium text-slate-700">参考答案</p>
                                <p className="mt-1 whitespace-pre-wrap">{question.referenceAnswer}</p>
                              </div>
                            ) : null}
                            {question.scoringRule ? (
                              <div className="mt-3">
                                <p className="font-medium text-slate-700">评分标准</p>
                                <p className="mt-1 whitespace-pre-wrap">{question.scoringRule}</p>
                              </div>
                            ) : null}
                            {question.followUpQuestions ? (
                              <div className="mt-3">
                                <p className="font-medium text-slate-700">追问</p>
                                <p className="mt-1 whitespace-pre-wrap">{question.followUpQuestions}</p>
                              </div>
                            ) : null}
                          </div>
                        ) : null}
                      </div>
                    ))}
                  </div>
                </div>
              </>
            ) : null}
          </div>
        </div>

        {generatedQuestions.length > 0 ? (
          <DialogFooter className="mt-6 shrink-0">
            <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isSaving}>
              取消
            </Button>
            <Button onClick={handleSaveAll} disabled={isSaving || generatedQuestions.length === 0}>
              <Save className="mr-1.5 h-4 w-4" />
              {isSaving ? "保存中..." : `全部保存 (${generatedQuestions.length})`}
            </Button>
          </DialogFooter>
        ) : null}
      </DialogContent>
    </Dialog>
  );
}

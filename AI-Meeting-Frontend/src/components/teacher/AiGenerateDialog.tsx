import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { Bot, Save, X } from "lucide-react";
import { teacherService } from "@/services/teacherService";
import type { QuestionCreateDTO, AiGenerateParams, AiPropertiesDTO } from "@/services/teacherService";
import type { CollegeRespDTO, MajorRespDTO } from "@/services/questionBankService";
import {
  QUESTION_TYPE_OPTIONS,
  DIFFICULTY_OPTIONS,
  ABILITY_TAG_OPTIONS,
} from "@/services/questionBankService";

interface AiGenerateDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onBatchSave: (questions: QuestionCreateDTO[]) => void;
  isSaving: boolean;
}

export default function AiGenerateDialog({
  open,
  onOpenChange,
  onBatchSave,
  isSaving,
}: AiGenerateDialogProps) {
  const [collegeId, setCollegeId] = useState<number | "">("");
  const [majorId, setMajorId] = useState<number | "">("");
  const [questionType, setQuestionType] = useState("");
  const [abilityTag, setAbilityTag] = useState("");
  const [count, setCount] = useState(5);
  const [difficulty, setDifficulty] = useState("");
  const [generateFollowUp, setGenerateFollowUp] = useState(false);
  const [generateScoringRule, setGenerateScoringRule] = useState(false);

  const [colleges, setColleges] = useState<CollegeRespDTO[]>([]);
  const [majors, setMajors] = useState<MajorRespDTO[]>([]);
  const [aiModels, setAiModels] = useState<AiPropertiesDTO[]>([]);
  const [selectedModelId, setSelectedModelId] = useState<number | "">("");

  const [generatedQuestions, setGeneratedQuestions] = useState<QuestionCreateDTO[]>([]);
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load colleges and AI models
  useEffect(() => {
    if (open) {
      teacherService.listColleges().then(setColleges).catch(() => {});
      teacherService.getEnabledAiProperties().then(setAiModels).catch(() => {});
      resetState();
    }
  }, [open]);

  // Load majors when college changes (async fetch only)
  useEffect(() => {
    if (!collegeId) return;
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

  function resetState() {
    setGeneratedQuestions([]);
    setError(null);
    setSelectedModelId("");
  }

  function handleCollegeChange(value: string) {
    const numVal = value ? Number(value) : "";
    setCollegeId(numVal);
    setMajors([]);
    setMajorId("");
  }

  async function handleGenerate() {
    setIsGenerating(true);
    setError(null);
    setGeneratedQuestions([]);

    try {
      const params: AiGenerateParams = {
        collegeId: collegeId ? (collegeId as number) : undefined,
        majorId: majorId ? (majorId as number) : undefined,
        questionType: questionType || undefined,
        abilityTag: abilityTag || undefined,
        count,
        difficulty: difficulty || undefined,
        generateFollowUp,
        generateScoringRule,
        aiPropertiesId: selectedModelId ? (selectedModelId as number) : undefined,
      };

      const result = await teacherService.aiGenerateQuestions(params);
      setGeneratedQuestions(Array.isArray(result) ? result : (result.questions ?? []));
    } catch (err) {
      setError(err instanceof Error ? err.message : "AI 出题失败，请重试");
    } finally {
      setIsGenerating(false);
    }
  }

  function handleRemoveQuestion(index: number) {
    setGeneratedQuestions((prev) => prev.filter((_, i) => i !== index));
  }

  function handleSaveAll() {
    if (generatedQuestions.length === 0) return;
    const questionsWithMeta = generatedQuestions.map((q) => ({
      ...q,
      isAiGenerated: true,
      status: "pending_review" as const,
    }));
    onBatchSave(questionsWithMeta);
  }

  function getQuestionTypeLabel(value?: string): string {
    if (!value) return "-";
    return QUESTION_TYPE_OPTIONS.find((o) => o.value === value)?.label ?? value;
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Bot className="h-5 w-5 text-violet-600" />
            AI 智能出题
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          {/* Config form */}
          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <Label>院校</Label>
              <select
                value={collegeId}
                onChange={(e) => handleCollegeChange(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              >
                <option value="">不限</option>
                {colleges.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label>专业</Label>
              <select
                value={majorId}
                onChange={(e) =>
                  setMajorId(
                    e.target.value ? Number(e.target.value) : "",
                  )
                }
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                disabled={!collegeId}
              >
                <option value="">不限</option>
                {majors.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label>AI 模型</Label>
              <select
                value={selectedModelId}
                onChange={(e) =>
                  setSelectedModelId(e.target.value ? Number(e.target.value) : "")
                }
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              >
                <option value="">系统默认</option>
                {aiModels.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.aiName} ({m.modelName})
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
                onChange={(e) => setQuestionType(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              >
                <option value="">请选择题型</option>
                {QUESTION_TYPE_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label>能力点</Label>
              <select
                value={abilityTag}
                onChange={(e) => setAbilityTag(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              >
                <option value="">不限</option>
                {ABILITY_TAG_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label>难度</Label>
              <select
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value)}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              >
                <option value="">不限</option>
                {DIFFICULTY_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <Label>数量 (1-20)</Label>
              <Input
                type="number"
                value={count}
                onChange={(e) =>
                  setCount(
                    Math.max(1, Math.min(20, Number(e.target.value) || 1)),
                  )
                }
                min={1}
                max={20}
              />
            </div>
            <div className="flex items-end gap-4 pb-1.5">
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={generateFollowUp}
                  onChange={(e) => setGenerateFollowUp(e.target.checked)}
                  className="h-4 w-4 rounded border-slate-300"
                />
                生成追问
              </label>
            </div>
            <div className="flex items-end gap-4 pb-1.5">
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={generateScoringRule}
                  onChange={(e) => setGenerateScoringRule(e.target.checked)}
                  className="h-4 w-4 rounded border-slate-300"
                />
                生成评分标准
              </label>
            </div>
          </div>

          <Button
            onClick={handleGenerate}
            disabled={isGenerating || !questionType}
            className="w-full"
          >
            {isGenerating ? "AI 生成中..." : "开始生成"}
          </Button>

          {error && (
            <p className="text-sm text-red-500">{error}</p>
          )}

          {/* Generated results */}
          {generatedQuestions.length > 0 && (
            <>
              <Separator />
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-medium text-slate-800">
                    生成结果 ({generatedQuestions.length} 道)
                  </h3>
                </div>
                <ScrollArea className="max-h-[300px]">
                  <div className="space-y-3 pr-4">
                    {generatedQuestions.map((q, idx) => (
                      <div
                        key={idx}
                        className="rounded-md border border-slate-200 bg-slate-50 p-3"
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex-1">
                            <p className="text-sm font-medium text-slate-800">
                              {q.title}
                            </p>
                            <p className="mt-1 text-xs text-slate-500">
                              {getQuestionTypeLabel(q.questionType)}
                              {q.difficulty &&
                                ` | ${DIFFICULTY_OPTIONS.find((d) => d.value === q.difficulty)?.label ?? q.difficulty}`}
                            </p>
                            {q.content && (
                              <p className="mt-1.5 text-xs text-slate-600 line-clamp-2">
                                {q.content}
                              </p>
                            )}
                          </div>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-6 w-6 shrink-0 text-slate-400 hover:text-red-500"
                            onClick={() => handleRemoveQuestion(idx)}
                          >
                            <X className="h-3.5 w-3.5" />
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </ScrollArea>
              </div>
            </>
          )}
        </div>

        {generatedQuestions.length > 0 && (
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isSaving}
            >
              取消
            </Button>
            <Button
              onClick={handleSaveAll}
              disabled={isSaving || generatedQuestions.length === 0}
            >
              <Save className="mr-1.5 h-4 w-4" />
              {isSaving
                ? "保存中..."
                : `全部保存 (${generatedQuestions.length})`}
            </Button>
          </DialogFooter>
        )}
      </DialogContent>
    </Dialog>
  );
}

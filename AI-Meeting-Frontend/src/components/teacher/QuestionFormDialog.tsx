import { useCallback, useEffect, useRef, useState } from "react";
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
import { Textarea } from "@/components/ui/textarea";
import { ScrollArea } from "@/components/ui/scroll-area";
import { teacherService } from "@/services/teacherService";
import type { QuestionCreateDTO } from "@/services/teacherService";
import type { QuestionRespDTO, CollegeRespDTO, MajorRespDTO } from "@/services/questionBankService";
import {
  QUESTION_TYPE_OPTIONS,
  DIFFICULTY_OPTIONS,
  ABILITY_TAG_OPTIONS,
} from "@/services/questionBankService";

interface QuestionFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: QuestionCreateDTO) => void;
  isSubmitting: boolean;
  editingQuestion?: QuestionRespDTO | null;
}

export default function QuestionFormDialog({
  open,
  onOpenChange,
  onSubmit,
  isSubmitting,
  editingQuestion,
}: QuestionFormDialogProps) {
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [questionType, setQuestionType] = useState("");
  const [collegeId, setCollegeId] = useState<number | "">("");
  const [majorId, setMajorId] = useState<number | "">("");
  const [abilityTag, setAbilityTag] = useState("");
  const [difficulty, setDifficulty] = useState("");
  const [answerTimeSeconds, setAnswerTimeSeconds] = useState<number | "">("");
  const [referenceAnswer, setReferenceAnswer] = useState("");
  const [scoringRule, setScoringRule] = useState("");
  const [followUpRule, setFollowUpRule] = useState("");

  const [colleges, setColleges] = useState<CollegeRespDTO[]>([]);
  const [majors, setMajors] = useState<MajorRespDTO[]>([]);

  const didInitRef = useRef(false);

  // Load colleges on open
  useEffect(() => {
    if (open) {
      teacherService.listColleges().then(setColleges).catch(() => {});
    }
  }, [open]);

  // Populate form when dialog opens (runs once per open)
  useEffect(() => {
    if (!open) {
      didInitRef.current = false;
      return;
    }
    if (didInitRef.current) return;
    didInitRef.current = true;

    const eq = editingQuestion;
    if (eq) {
      /* eslint-disable react-hooks/set-state-in-effect -- form init from props */
      setTitle(eq.title ?? "");
      setContent(eq.content ?? "");
      setQuestionType(eq.questionType ?? "");
      setCollegeId(eq.collegeId ?? "");
      setMajorId(eq.majorId ?? "");
      setAbilityTag(eq.abilityTag ?? "");
      setDifficulty(eq.difficulty ?? "");
      setAnswerTimeSeconds(eq.answerTimeSeconds ?? "");
      setReferenceAnswer(eq.referenceAnswer ?? "");
      setScoringRule(eq.scoringRule ?? "");
      setFollowUpRule(eq.followUpRule ?? "");
      /* eslint-enable react-hooks/set-state-in-effect */
    } else {
      setTitle("");
      setContent("");
      setQuestionType("");
      setCollegeId("");
      setMajorId("");
      setAbilityTag("");
      setDifficulty("");
      setAnswerTimeSeconds("");
      setReferenceAnswer("");
      setScoringRule("");
      setFollowUpRule("");
    }
  }, [open, editingQuestion]);

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

  const handleCollegeChange = useCallback(
    (value: string) => {
      const numVal = value ? Number(value) : "";
      setCollegeId(numVal);
      setMajors([]);
      setMajorId("");
    },
    [],
  );

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !questionType) return;

    onSubmit({
      title: title.trim(),
      content: content.trim() || undefined,
      questionType,
      collegeId: collegeId ? (collegeId as number) : undefined,
      majorId: majorId ? (majorId as number) : undefined,
      abilityTag: abilityTag || undefined,
      difficulty: difficulty || undefined,
      answerTimeSeconds:
        answerTimeSeconds ? (answerTimeSeconds as number) : undefined,
      referenceAnswer: referenceAnswer.trim() || undefined,
      scoringRule: scoringRule.trim() || undefined,
      followUpRule: followUpRule.trim() || undefined,
    });
  }

  const isEditing = !!editingQuestion;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            {isEditing ? "编辑题目" : "新建题目"}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit}>
          <ScrollArea className="max-h-[60vh]">
            <div className="space-y-4 pr-4">
              {/* Title */}
              <div className="space-y-1.5">
                <Label>
                  标题 <span className="text-red-500">*</span>
                </Label>
                <Input
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="请输入题目标题"
                  required
                />
              </div>

              {/* Content */}
              <div className="space-y-1.5">
                <Label>正文</Label>
                <Textarea
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  placeholder="请输入题目正文描述"
                  rows={3}
                />
              </div>

              {/* Question type + Difficulty */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label>
                    题型 <span className="text-red-500">*</span>
                  </Label>
                  <select
                    value={questionType}
                    onChange={(e) => setQuestionType(e.target.value)}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    required
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
                  <Label>难度</Label>
                  <select
                    value={difficulty}
                    onChange={(e) => setDifficulty(e.target.value)}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  >
                    <option value="">请选择难度</option>
                    {DIFFICULTY_OPTIONS.map((o) => (
                      <option key={o.value} value={o.value}>
                        {o.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* College + Major */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label>院校</Label>
                  <select
                    value={collegeId}
                    onChange={(e) => handleCollegeChange(e.target.value)}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  >
                    <option value="">全部院校</option>
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
                    <option value="">全部专业</option>
                    {majors.map((m) => (
                      <option key={m.id} value={m.id}>
                        {m.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Ability tag + Answer time */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label>能力点</Label>
                  <select
                    value={abilityTag}
                    onChange={(e) => setAbilityTag(e.target.value)}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  >
                    <option value="">请选择能力点</option>
                    {ABILITY_TAG_OPTIONS.map((o) => (
                      <option key={o.value} value={o.value}>
                        {o.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="space-y-1.5">
                  <Label>作答时间（秒）</Label>
                  <Input
                    type="number"
                    value={answerTimeSeconds}
                    onChange={(e) =>
                      setAnswerTimeSeconds(
                        e.target.value ? Number(e.target.value) : "",
                      )
                    }
                    placeholder="例如：120"
                    min={10}
                    max={600}
                  />
                </div>
              </div>

              {/* Reference answer */}
              <div className="space-y-1.5">
                <Label>参考答案</Label>
                <Textarea
                  value={referenceAnswer}
                  onChange={(e) => setReferenceAnswer(e.target.value)}
                  placeholder="请输入参考答案"
                  rows={3}
                />
              </div>

              {/* Scoring rule */}
              <div className="space-y-1.5">
                <Label>评分规则</Label>
                <Textarea
                  value={scoringRule}
                  onChange={(e) => setScoringRule(e.target.value)}
                  placeholder="请输入评分规则"
                  rows={2}
                />
              </div>

              {/* Follow-up rule */}
              <div className="space-y-1.5">
                <Label>追问规则</Label>
                <Textarea
                  value={followUpRule}
                  onChange={(e) => setFollowUpRule(e.target.value)}
                  placeholder="请输入追问规则"
                  rows={2}
                />
              </div>
            </div>
          </ScrollArea>

          <DialogFooter className="mt-6">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isSubmitting}
            >
              取消
            </Button>
            <Button type="submit" disabled={isSubmitting || !title.trim() || !questionType}>
              {isSubmitting ? "保存中..." : isEditing ? "保存修改" : "创建题目"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

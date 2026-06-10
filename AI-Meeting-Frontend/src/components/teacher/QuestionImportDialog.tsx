import { useCallback, useEffect, useState } from "react";
import { AlertCircle, CheckCircle2, FileText, Upload } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import { teacherService } from "@/services/teacherService";
import type {
  QuestionImportResult,
  QuestionImportPreviewParams,
} from "@/services/teacherService";
import type {
  CollegeRespDTO,
  MajorRespDTO,
} from "@/services/questionBankService";
import {
  DIFFICULTY_OPTIONS,
  QUESTION_TYPE_OPTIONS,
} from "@/services/questionBankService";

interface QuestionImportDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onImported: () => void;
}

export default function QuestionImportDialog({
  open,
  onOpenChange,
  onImported,
}: QuestionImportDialogProps) {
  const [file, setFile] = useState<File | null>(null);
  const [importType, setImportType] = useState<"word_table" | "word_section">("word_table");
  const [collegeId, setCollegeId] = useState<number | "">("");
  const [majorId, setMajorId] = useState<number | "">("");
  const [defaultQuestionType, setDefaultQuestionType] = useState("专业题");
  const [defaultDifficulty, setDefaultDifficulty] = useState("medium");
  const [defaultYear, setDefaultYear] = useState(2026);
  const [colleges, setColleges] = useState<CollegeRespDTO[]>([]);
  const [majors, setMajors] = useState<MajorRespDTO[]>([]);
  const [preview, setPreview] = useState<QuestionImportResult | null>(null);
  const [isParsing, setIsParsing] = useState(false);
  const [isConfirming, setIsConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    teacherService.listColleges().then(setColleges).catch(() => setColleges([]));
  }, [open]);

  useEffect(() => {
    if (!collegeId) {
      setMajors([]);
      setMajorId("");
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

  const handleCollegeChange = useCallback((value: string) => {
    setCollegeId(value ? Number(value) : "");
    setMajors([]);
    setMajorId("");
  }, []);

  const handlePreview = useCallback(async () => {
    if (!file) {
      setError("请先选择 .docx Word 文件");
      return;
    }
    if (!file.name.toLowerCase().endsWith(".docx")) {
      setError("当前只支持 .docx 文件，请先另存为 docx 后再上传");
      return;
    }

    setIsParsing(true);
    setError(null);
    setPreview(null);
    try {
      const params: QuestionImportPreviewParams = {
        file,
        importType,
        collegeId: collegeId ? (collegeId as number) : undefined,
        majorId: majorId ? (majorId as number) : undefined,
        defaultQuestionType,
        defaultDifficulty,
        defaultYear,
        statusAfterImport: "pending_review",
      };
      const result = await teacherService.previewQuestionImport(params);
      setPreview(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Word 解析失败，请检查模板格式");
    } finally {
      setIsParsing(false);
    }
  }, [
    collegeId,
    defaultDifficulty,
    defaultQuestionType,
    defaultYear,
    file,
    importType,
    majorId,
  ]);

  const handleConfirm = useCallback(async () => {
    if (!preview?.batchId || preview.validCount <= 0) {
      return;
    }
    setIsConfirming(true);
    setError(null);
    try {
      await teacherService.confirmQuestionImport(preview.batchId);
      onImported();
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "确认导入失败，请稍后重试");
    } finally {
      setIsConfirming(false);
    }
  }, [onImported, onOpenChange, preview]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Upload className="h-5 w-5 text-sky-600" />
            Word 批量导入题库
          </DialogTitle>
          <DialogDescription>
            上传约定格式的 .docx 文件，先预览解析结果，再确认写入题库审核池。
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 md:grid-cols-2">
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="question-import-file">Word 文件</Label>
              <Input
                id="question-import-file"
                type="file"
                accept=".docx"
                onChange={(event) => {
                  setFile(event.target.files?.[0] ?? null);
                  setPreview(null);
                  setError(null);
                }}
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="question-import-type">解析方式</Label>
                <select
                  id="question-import-type"
                  value={importType}
                  onChange={(event) =>
                    setImportType(event.target.value as "word_table" | "word_section")
                  }
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                  <option value="word_table">表格模板</option>
                  <option value="word_section">分段模板</option>
                </select>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="question-import-year">默认年份</Label>
                <Input
                  id="question-import-year"
                  type="number"
                  value={defaultYear}
                  onChange={(event) => setDefaultYear(Number(event.target.value) || 2026)}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="question-import-college">院校</Label>
                <select
                  id="question-import-college"
                  value={collegeId}
                  onChange={(event) => handleCollegeChange(event.target.value)}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                  <option value="">通用</option>
                  {colleges.map((college) => (
                    <option key={college.id} value={college.id}>
                      {college.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="question-import-major">专业</Label>
                <select
                  id="question-import-major"
                  value={majorId}
                  onChange={(event) => setMajorId(event.target.value ? Number(event.target.value) : "")}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  disabled={!collegeId}
                >
                  <option value="">通用</option>
                  {majors.map((major) => (
                    <option key={major.id} value={major.id}>
                      {major.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="question-import-question-type">默认题型</Label>
                <select
                  id="question-import-question-type"
                  value={defaultQuestionType}
                  onChange={(event) => setDefaultQuestionType(event.target.value)}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                  {QUESTION_TYPE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="question-import-difficulty">默认难度</Label>
                <select
                  id="question-import-difficulty"
                  value={defaultDifficulty}
                  onChange={(event) => setDefaultDifficulty(event.target.value)}
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                >
                  {DIFFICULTY_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <Button onClick={handlePreview} disabled={isParsing} className="w-full">
              {isParsing ? "解析中..." : "解析预览"}
            </Button>

            {error ? (
              <div className="flex gap-2 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
                <span>{error}</span>
              </div>
            ) : null}
          </div>

          <div className="min-w-0 rounded-md border border-slate-200 bg-slate-50 p-4">
            <div className="mb-3 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-medium text-slate-800">导入预览</h3>
                <p className="text-xs text-slate-500">
                  确认后会写入题库，默认进入 pending_review。
                </p>
              </div>
              <FileText className="h-5 w-5 text-slate-400" />
            </div>

            {preview ? (
              <div className="space-y-3">
                <div className="grid grid-cols-3 gap-2 text-center text-sm">
                  <div className="rounded-md bg-white p-2">
                    <div className="font-semibold text-slate-800">{preview.totalRows}</div>
                    <div className="text-xs text-slate-500">总行数</div>
                  </div>
                  <div className="rounded-md bg-emerald-50 p-2 text-emerald-700">
                    <div className="font-semibold">有效 {preview.validCount} 道</div>
                    <div className="text-xs">可导入</div>
                  </div>
                  <div className="rounded-md bg-amber-50 p-2 text-amber-700">
                    <div className="font-semibold">无效 {preview.invalidCount} 道</div>
                    <div className="text-xs">需修正</div>
                  </div>
                </div>

                <ScrollArea className="h-[300px]">
                  <div className="space-y-2 pr-3">
                    {preview.items.slice(0, 30).map((item) => (
                      <div
                        key={`${item.rowIndex}-${item.title ?? item.errorMessage}`}
                        className="rounded-md border border-slate-200 bg-white p-3"
                      >
                        <div className="flex items-start gap-2">
                          {item.valid ? (
                            <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" />
                          ) : (
                            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-amber-500" />
                          )}
                          <div className="min-w-0 flex-1">
                            <p className="break-words text-sm font-medium text-slate-800">
                              {item.title || `第 ${item.rowIndex} 行解析失败`}
                            </p>
                            <p className="mt-1 text-xs text-slate-500">
                              第 {item.rowIndex} 行 · {item.questionType || "未识别题型"}
                            </p>
                            {!item.valid && item.errorMessage ? (
                              <p className="mt-1 text-xs text-amber-700">
                                {item.errorMessage}
                              </p>
                            ) : null}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </ScrollArea>
              </div>
            ) : (
              <div className="flex h-[300px] items-center justify-center rounded-md border border-dashed border-slate-300 bg-white text-sm text-slate-500">
                选择 Word 文件后点击解析预览
              </div>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button
            onClick={handleConfirm}
            disabled={!preview || preview.validCount <= 0 || isConfirming}
          >
            {isConfirming ? "导入中..." : `确认导入 ${preview?.validCount ?? 0} 道题`}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

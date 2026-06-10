import { useState, useCallback } from "react";
import { Plus, Bot, Search, X, Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useTeacherQuestions } from "@/hooks/teacher/useTeacherQuestions";
import QuestionBankTable from "@/components/teacher/QuestionBankTable";
import QuestionFormDialog from "@/components/teacher/QuestionFormDialog";
import AiGenerateDialog from "@/components/teacher/AiGenerateDialog";
import QuestionImportDialog from "@/components/teacher/QuestionImportDialog";
import type { QuestionRespDTO, CollegeRespDTO, MajorRespDTO } from "@/services/questionBankService";
import type { QuestionCreateDTO } from "@/services/teacherService";
import { teacherService } from "@/services/teacherService";
import {
  QUESTION_TYPE_OPTIONS,
  DIFFICULTY_OPTIONS,
} from "@/services/questionBankService";
import { useEffect } from "react";

export default function TeacherQuestionsPage() {
  const {
    questions,
    total,
    totalPages,
    currentPage,
    isLoading,
    isFetching,
    setPage,
    refetch,
    updateFilters,
    createQuestion,
    updateQuestion,
    deleteQuestion,
  } = useTeacherQuestions();

  // Dialog states
  const [formOpen, setFormOpen] = useState(false);
  const [aiOpen, setAiOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState<QuestionRespDTO | null>(null);

  // Filter data
  const [colleges, setColleges] = useState<CollegeRespDTO[]>([]);
  const [majors, setMajors] = useState<MajorRespDTO[]>([]);
  const [collegeId, setCollegeId] = useState<number | "">("");
  const [majorId, setMajorId] = useState<number | "">("");
  const [questionType, setQuestionType] = useState("");
  const [difficulty, setDifficulty] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  // Load colleges on mount
  useEffect(() => {
    teacherService.listColleges().then(setColleges).catch(() => {});
  }, []);

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

  // ── Handlers ──
  const handleCollegeChange = useCallback((value: string) => {
    const numVal = value ? Number(value) : "";
    setCollegeId(numVal);
    setMajors([]);
    setMajorId("");
  }, []);

  const handleApplyFilters = useCallback(() => {
    updateFilters({
      collegeId: collegeId ? (collegeId as number) : undefined,
      majorId: majorId ? (majorId as number) : undefined,
      questionType: questionType || undefined,
      difficulty: difficulty || undefined,
      status: statusFilter || undefined,
    });
  }, [collegeId, majorId, questionType, difficulty, statusFilter, updateFilters]);

  const handleClearFilters = useCallback(() => {
    setCollegeId("");
    setMajors([]);
    setMajorId("");
    setQuestionType("");
    setDifficulty("");
    setStatusFilter("");
    updateFilters({
      collegeId: undefined,
      majorId: undefined,
      questionType: undefined,
      difficulty: undefined,
      status: undefined,
    });
  }, [updateFilters]);

  const handleCreate = useCallback(() => {
    setEditingQuestion(null);
    setFormOpen(true);
  }, []);

  const handleEdit = useCallback((q: QuestionRespDTO) => {
    setEditingQuestion(q);
    setFormOpen(true);
  }, []);

  const handleDelete = useCallback(
    (q: QuestionRespDTO) => {
      if (window.confirm(`确定删除题目「${q.title}」吗？`)) {
        deleteQuestion.mutate(q.id, {
          onSuccess: () => window.alert("删除成功"),
          onError: () => window.alert("删除失败，请重试"),
        });
      }
    },
    [deleteQuestion],
  );

  const handleFormSubmit = useCallback(
    (data: QuestionCreateDTO) => {
      if (editingQuestion) {
        updateQuestion.mutate(
          { id: editingQuestion.id, data },
          {
            onSuccess: () => {
              setFormOpen(false);
              setEditingQuestion(null);
              window.alert("更新成功");
            },
            onError: () => window.alert("更新失败，请重试"),
          },
        );
      } else {
        createQuestion.mutate(data, {
          onSuccess: () => {
            setFormOpen(false);
            window.alert("创建成功");
          },
          onError: () => window.alert("创建失败，请重试"),
        });
      }
    },
    [editingQuestion, createQuestion, updateQuestion],
  );

  const handleAiBatchSave = useCallback(
    (questionsToSave: QuestionCreateDTO[]) => {
      let saved = 0;
      let failed = 0;
      const totalToSave = questionsToSave.length;

      const saveNext = (index: number) => {
        if (index >= totalToSave) {
          setAiOpen(false);
          window.alert(
            `保存完成：成功 ${saved} 道，失败 ${failed} 道`,
          );
          return;
        }

        createQuestion.mutate(questionsToSave[index], {
          onSuccess: () => {
            saved++;
            saveNext(index + 1);
          },
          onError: () => {
            failed++;
            saveNext(index + 1);
          },
        });
      };

      saveNext(0);
    },
    [createQuestion],
  );

  const isSubmitting =
    createQuestion.isPending || updateQuestion.isPending;

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">题库管理</h1>
          <p className="mt-0.5 text-sm text-slate-500">
            管理面试题目，支持手动创建和 AI 智能出题
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => setImportOpen(true)}>
            <Upload className="mr-1.5 h-4 w-4" />
            Word 导入
          </Button>
          <Button variant="outline" onClick={() => setAiOpen(true)}>
            <Bot className="mr-1.5 h-4 w-4" />
            AI 出题
          </Button>
          <Button onClick={handleCreate}>
            <Plus className="mr-1.5 h-4 w-4" />
            新建题目
          </Button>
        </div>
      </div>

      {/* Filters */}
      <div className="border-b border-slate-200 bg-slate-50/50 px-6 py-3">
        <div className="flex flex-wrap items-end gap-3">
          <div className="space-y-1">
            <label className="text-xs text-slate-500">院校</label>
            <select
              value={collegeId}
              onChange={(e) => handleCollegeChange(e.target.value)}
              className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">全部</option>
              {colleges.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs text-slate-500">专业</label>
            <select
              value={majorId}
              onChange={(e) =>
                setMajorId(
                  e.target.value ? Number(e.target.value) : "",
                )
              }
              className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              disabled={!collegeId}
            >
              <option value="">全部</option>
              {majors.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs text-slate-500">题型</label>
            <select
              value={questionType}
              onChange={(e) => setQuestionType(e.target.value)}
              className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">全部</option>
              {QUESTION_TYPE_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs text-slate-500">难度</label>
            <select
              value={difficulty}
              onChange={(e) => setDifficulty(e.target.value)}
              className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">全部</option>
              {DIFFICULTY_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs text-slate-500">状态</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">全部</option>
              <option value="active">启用</option>
              <option value="inactive">停用</option>
              <option value="draft">草稿</option>
            </select>
          </div>
          <div className="flex items-end gap-2">
            <Button size="sm" onClick={handleApplyFilters}>
              <Search className="mr-1 h-3.5 w-3.5" />
              筛选
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleClearFilters}
            >
              <X className="mr-1 h-3.5 w-3.5" />
              清除
            </Button>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="flex-1 overflow-auto p-6">
        <QuestionBankTable
          questions={questions}
          total={total}
          currentPage={currentPage}
          totalPages={totalPages}
          isLoading={isLoading || isFetching}
          onPageChange={setPage}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      </div>

      {/* Dialogs */}
      <QuestionFormDialog
        open={formOpen}
        onOpenChange={(open) => {
          setFormOpen(open);
          if (!open) setEditingQuestion(null);
        }}
        onSubmit={handleFormSubmit}
        isSubmitting={isSubmitting}
        editingQuestion={editingQuestion}
      />

      <AiGenerateDialog
        open={aiOpen}
        onOpenChange={setAiOpen}
        onBatchSave={handleAiBatchSave}
        isSaving={createQuestion.isPending}
      />

      <QuestionImportDialog
        open={importOpen}
        onOpenChange={setImportOpen}
        onImported={() => {
          void refetch();
          window.alert("导入成功，题目已进入待审核状态");
        }}
      />
    </div>
  );
}

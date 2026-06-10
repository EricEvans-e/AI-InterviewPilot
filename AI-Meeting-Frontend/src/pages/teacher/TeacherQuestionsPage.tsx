import { useCallback, useEffect, useMemo, useState } from "react";
import { Bot, Plus, Search, Upload, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import QuestionBankTable from "@/components/teacher/QuestionBankTable";
import AiExpandDialog from "@/components/teacher/AiExpandDialog";
import AiGenerateDialog from "@/components/teacher/AiGenerateDialog";
import QuestionFormDialog from "@/components/teacher/QuestionFormDialog";
import QuestionImportDialog from "@/components/teacher/QuestionImportDialog";
import { useTeacherQuestions } from "@/hooks/teacher/useTeacherQuestions";
import {
  DIFFICULTY_OPTIONS,
  QUESTION_TYPE_OPTIONS,
  type CollegeRespDTO,
  type MajorRespDTO,
  type QuestionRespDTO,
} from "@/services/questionBankService";
import { teacherService, type QuestionCreateDTO } from "@/services/teacherService";

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

  const [formOpen, setFormOpen] = useState(false);
  const [aiOpen, setAiOpen] = useState(false);
  const [aiExpandOpen, setAiExpandOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState<QuestionRespDTO | null>(null);
  const [expandQuestionIds, setExpandQuestionIds] = useState<number[]>([]);

  const [colleges, setColleges] = useState<CollegeRespDTO[]>([]);
  const [majors, setMajors] = useState<MajorRespDTO[]>([]);
  const [allMajors, setAllMajors] = useState<MajorRespDTO[]>([]);
  const [collegeId, setCollegeId] = useState<number | "">("");
  const [majorId, setMajorId] = useState<number | "">("");
  const [titleKeyword, setTitleKeyword] = useState("");
  const [questionType, setQuestionType] = useState("");
  const [difficulty, setDifficulty] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  const selectedExpandQuestions = useMemo(
    () => questions.filter((question) => expandQuestionIds.includes(question.id)),
    [expandQuestionIds, questions],
  );

  useEffect(() => {
    let cancelled = false;
    Promise.all([teacherService.listColleges(), teacherService.listMajors()])
      .then(([collegeList, majorList]) => {
        if (cancelled) return;
        setColleges(collegeList);
        setAllMajors(majorList);
      })
      .catch(() => {
        if (cancelled) return;
        setColleges([]);
        setAllMajors([]);
      });

    return () => {
      cancelled = true;
    };
  }, []);

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

  const handleCollegeChange = useCallback((value: string) => {
    const nextCollegeId = value ? Number(value) : "";
    setCollegeId(nextCollegeId);
    setMajors([]);
    setMajorId("");
  }, []);

  const handleApplyFilters = useCallback(() => {
    updateFilters({
      collegeId: collegeId ? (collegeId as number) : undefined,
      majorId: majorId ? (majorId as number) : undefined,
      titleKeyword: titleKeyword.trim() || undefined,
      questionType: questionType || undefined,
      difficulty: difficulty || undefined,
      status: statusFilter || undefined,
    });
  }, [collegeId, difficulty, majorId, questionType, statusFilter, titleKeyword, updateFilters]);

  const handleClearFilters = useCallback(() => {
    setCollegeId("");
    setMajors([]);
    setMajorId("");
    setTitleKeyword("");
    setQuestionType("");
    setDifficulty("");
    setStatusFilter("");
    updateFilters({
      collegeId: undefined,
      majorId: undefined,
      titleKeyword: undefined,
      questionType: undefined,
      difficulty: undefined,
      status: undefined,
    });
  }, [updateFilters]);

  const handleCreate = useCallback(() => {
    setEditingQuestion(null);
    setFormOpen(true);
  }, []);

  const handleEdit = useCallback((question: QuestionRespDTO) => {
    setEditingQuestion(question);
    setFormOpen(true);
  }, []);

  const handleDelete = useCallback(
    (question: QuestionRespDTO) => {
      if (!window.confirm(`确定删除题目「${question.title}」吗？`)) {
        return;
      }
      deleteQuestion.mutate(question.id, {
        onSuccess: () => window.alert("删除成功"),
        onError: () => window.alert("删除失败，请重试"),
      });
    },
    [deleteQuestion],
  );

  const finishReviewRefresh = useCallback(async () => {
    setSelectedIds([]);
    await refetch();
  }, [refetch]);

  const handleApprove = useCallback(
    async (question: QuestionRespDTO) => {
      try {
        await teacherService.updateQuestionStatus(question.id, "approved");
        window.alert("审核通过");
        await finishReviewRefresh();
      } catch {
        window.alert("审核失败，请重试");
      }
    },
    [finishReviewRefresh],
  );

  const handleReject = useCallback(
    async (question: QuestionRespDTO) => {
      try {
        await teacherService.updateQuestionStatus(question.id, "rejected");
        window.alert("已拒绝题目");
        await finishReviewRefresh();
      } catch {
        window.alert("审核失败，请重试");
      }
    },
    [finishReviewRefresh],
  );

  const handleBatchApprove = useCallback(
    async (ids: number[]) => {
      if (ids.length === 0) return;
      try {
        await teacherService.batchUpdateQuestionStatus(ids, "approved");
        window.alert(`已批量通过 ${ids.length} 道题目`);
        await finishReviewRefresh();
      } catch {
        window.alert("批量审核失败，请重试");
      }
    },
    [finishReviewRefresh],
  );

  const handleBatchReject = useCallback(
    async (ids: number[]) => {
      if (ids.length === 0) return;
      try {
        await teacherService.batchUpdateQuestionStatus(ids, "rejected");
        window.alert(`已批量拒绝 ${ids.length} 道题目`);
        await finishReviewRefresh();
      } catch {
        window.alert("批量审核失败，请重试");
      }
    },
    [finishReviewRefresh],
  );

  const handleBatchDelete = useCallback(
    async (ids: number[]) => {
      if (ids.length === 0) return;
      if (!window.confirm(`确定删除选中的 ${ids.length} 道题目吗？`)) {
        return;
      }

      const results = await Promise.allSettled(ids.map((id) => teacherService.deleteQuestion(id)));
      const failedIds = results
        .map((result, index) => (result.status === "rejected" ? ids[index] : null))
        .filter((id): id is number => id !== null);

      await refetch();

      if (failedIds.length === 0) {
        setSelectedIds([]);
        window.alert(`已批量删除 ${ids.length} 道题目`);
        return;
      }

      setSelectedIds(failedIds);
      window.alert(
        `批量删除部分失败：成功 ${ids.length - failedIds.length} 道，失败 ${failedIds.length} 道，请重试失败项`,
      );
    },
    [refetch],
  );

  const handleBatchExpand = useCallback((ids: number[]) => {
    if (ids.length === 0) {
      return;
    }
    setExpandQuestionIds(ids);
    setAiExpandOpen(true);
  }, []);

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
        return;
      }

      createQuestion.mutate(data, {
        onSuccess: () => {
          setFormOpen(false);
          window.alert("创建成功");
        },
        onError: () => window.alert("创建失败，请重试"),
      });
    },
    [createQuestion, editingQuestion, updateQuestion],
  );

  const handleAiBatchSave = useCallback(
    (questionsToSave: QuestionCreateDTO[]) => {
      let saved = 0;
      let failed = 0;

      const saveNext = (index: number) => {
        if (index >= questionsToSave.length) {
          setAiOpen(false);
          setAiExpandOpen(false);
          void refetch();
          window.alert(`保存完成：成功 ${saved} 道，失败 ${failed} 道`);
          return;
        }

        createQuestion.mutate(questionsToSave[index], {
          onSuccess: () => {
            saved += 1;
            saveNext(index + 1);
          },
          onError: () => {
            failed += 1;
            saveNext(index + 1);
          },
        });
      };

      saveNext(0);
    },
    [createQuestion, refetch],
  );

  const isSubmitting = createQuestion.isPending || updateQuestion.isPending;

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">题库管理</h1>
          <p className="mt-0.5 text-sm text-slate-500">管理面试题目，支持手动创建和 AI 智能出题</p>
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

      <div className="border-b border-slate-200 bg-slate-50/50 px-6 py-3">
        <div className="flex flex-wrap items-end gap-3">
          <div className="space-y-1">
            <label className="text-xs text-slate-500">题目标题</label>
            <input
              value={titleKeyword}
              onChange={(event) => setTitleKeyword(event.target.value)}
              placeholder="按题目标题搜索"
              className="flex h-9 min-w-56 rounded-md border border-input bg-white px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs text-slate-500">院校</label>
            <select
              value={collegeId}
              onChange={(event) => handleCollegeChange(event.target.value)}
              className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">全部</option>
              {colleges.map((college) => (
                <option key={college.id} value={college.id}>
                  {college.name}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs text-slate-500">专业</label>
            <select
              value={majorId}
              onChange={(event) => setMajorId(event.target.value ? Number(event.target.value) : "")}
              className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              disabled={!collegeId}
            >
              <option value="">全部</option>
              {majors.map((major) => (
                <option key={major.id} value={major.id}>
                  {major.name}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs text-slate-500">题型</label>
            <select
              value={questionType}
              onChange={(event) => setQuestionType(event.target.value)}
              className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">全部</option>
              {QUESTION_TYPE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs text-slate-500">难度</label>
            <select
              value={difficulty}
              onChange={(event) => setDifficulty(event.target.value)}
              className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">全部</option>
              {DIFFICULTY_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="text-xs text-slate-500">状态</label>
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
              className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">全部</option>
              <option value="approved">启用</option>
              <option value="pending_review">待审核</option>
              <option value="draft">草稿</option>
              <option value="rejected">已拒绝</option>
            </select>
          </div>
          <div className="flex items-end gap-2">
            <Button size="sm" onClick={handleApplyFilters}>
              <Search className="mr-1 h-3.5 w-3.5" />
              筛选
            </Button>
            <Button variant="ghost" size="sm" onClick={handleClearFilters}>
              <X className="mr-1 h-3.5 w-3.5" />
              清除
            </Button>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-auto p-6">
        <QuestionBankTable
          questions={questions}
          colleges={colleges}
          majors={allMajors}
          total={total}
          currentPage={currentPage}
          totalPages={totalPages}
          isLoading={isLoading || isFetching}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onClearSelection={() => setSelectedIds([])}
          onBatchApprove={handleBatchApprove}
          onBatchExpand={handleBatchExpand}
          onBatchDelete={handleBatchDelete}
          onBatchReject={handleBatchReject}
          onApprove={handleApprove}
          onReject={handleReject}
          onPageChange={setPage}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      </div>

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

      <AiExpandDialog
        open={aiExpandOpen}
        onOpenChange={setAiExpandOpen}
        selectedQuestionIds={expandQuestionIds}
        selectedQuestions={selectedExpandQuestions}
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

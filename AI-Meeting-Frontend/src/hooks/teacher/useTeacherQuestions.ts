import { useState, useCallback } from "react";
import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";
import { teacherService } from "@/services/teacherService";
import type {
  QuestionCreateDTO,
  QuestionUpdateDTO,
  AiGenerateParams,
} from "@/services/teacherService";
import type { QuestionPageParams } from "@/services/questionBankService";

const QUESTIONS_QUERY_KEY = "teacher-questions";
const PAGE_SIZE = 15;

export function useTeacherQuestions() {
  const queryClient = useQueryClient();

  const [filters, setFilters] = useState<QuestionPageParams>({
    pageNum: 1,
    pageSize: PAGE_SIZE,
  });

  // ── Query ──
  const { data, isLoading, isFetching, refetch } = useQuery({
    queryKey: [QUESTIONS_QUERY_KEY, filters],
    queryFn: () => teacherService.pageQuestions(filters),
  });

  const questions = data?.records ?? [];
  const total = data?.total ?? 0;
  const totalPages = data?.pages ?? 0;

  // ── Mutations ──
  const createMutation = useMutation({
    mutationFn: (data: QuestionCreateDTO) =>
      teacherService.createQuestion(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUESTIONS_QUERY_KEY] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: QuestionUpdateDTO }) =>
      teacherService.updateQuestion(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUESTIONS_QUERY_KEY] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => teacherService.deleteQuestion(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [QUESTIONS_QUERY_KEY] });
    },
  });

  const aiGenerateMutation = useMutation({
    mutationFn: (params: AiGenerateParams) =>
      teacherService.aiGenerateQuestions(params),
  });

  // ── Helpers ──
  const setPage = useCallback((page: number) => {
    setFilters((prev) => ({ ...prev, pageNum: page }));
  }, []);

  const updateFilters = useCallback(
    (patch: Partial<QuestionPageParams>) => {
      setFilters((prev) => ({ ...prev, ...patch, pageNum: 1 }));
    },
    [],
  );

  return {
    // Data
    questions,
    total,
    totalPages,
    currentPage: filters.pageNum ?? 1,
    pageSize: PAGE_SIZE,
    filters,

    // Loading
    isLoading,
    isFetching,

    // Actions
    setPage,
    updateFilters,
    refetch,

    // Mutations
    createQuestion: createMutation,
    updateQuestion: updateMutation,
    deleteQuestion: deleteMutation,
    aiGenerate: aiGenerateMutation,
  };
}

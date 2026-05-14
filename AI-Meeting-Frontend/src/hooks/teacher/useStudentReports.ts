import { useState, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { teacherService } from "@/services/teacherService";
import type {
  StudentReportPageParams,
  ReviewSubmitDTO,
} from "@/services/teacherService";

const REPORTS_QUERY_KEY = "teacher-student-reports";
const PAGE_SIZE = 15;

export function useStudentReports() {
  const queryClient = useQueryClient();

  const [filters, setFilters] = useState<StudentReportPageParams>({
    pageNum: 1,
    pageSize: PAGE_SIZE,
  });

  // ── Query ──
  const { data, isLoading, isFetching, refetch } = useQuery({
    queryKey: [REPORTS_QUERY_KEY, filters],
    queryFn: () => teacherService.pageStudentReports(filters),
  });

  const reports = data?.records ?? [];
  const total = data?.total ?? 0;
  const totalPages = data?.pages ?? 0;

  // ── Mutations ──
  const submitReviewMutation = useMutation({
    mutationFn: (data: ReviewSubmitDTO) => teacherService.submitReview(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [REPORTS_QUERY_KEY] });
    },
  });

  // ── Helpers ──
  const setPage = useCallback((page: number) => {
    setFilters((prev) => ({ ...prev, pageNum: page }));
  }, []);

  const updateFilters = useCallback(
    (patch: Partial<StudentReportPageParams>) => {
      setFilters((prev) => ({ ...prev, ...patch, pageNum: 1 }));
    },
    [],
  );

  return {
    // Data
    reports,
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
    submitReview: submitReviewMutation,
  };
}

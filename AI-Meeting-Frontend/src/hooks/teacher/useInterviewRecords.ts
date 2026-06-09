import { useState, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { teacherService } from "@/services/teacherService";
import type { ReviewSubmitDTO } from "@/services/teacherService";

const RECORDS_QUERY_KEY = "teacher-interview-records";
const REPORTS_QUERY_KEY = "teacher-student-reports";
const PAGE_SIZE = 15;

export function useInterviewRecords() {
  const queryClient = useQueryClient();

  const [pageNum, setPageNum] = useState(1);

  const { data, isLoading, isFetching, refetch } = useQuery({
    queryKey: [RECORDS_QUERY_KEY, pageNum],
    queryFn: () => teacherService.pageInterviewRecords({ pageNum, pageSize: PAGE_SIZE }),
  });

  const records = data?.records ?? [];
  const total = data?.total ?? 0;
  const totalPages = data?.pages ?? 0;

  const submitReviewMutation = useMutation({
    mutationFn: (data: ReviewSubmitDTO) => teacherService.submitReview(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [REPORTS_QUERY_KEY] });
    },
  });

  const deleteRecordMutation = useMutation({
    mutationFn: (sessionId: string) => teacherService.deleteInterviewRecord(sessionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [RECORDS_QUERY_KEY] });
      queryClient.invalidateQueries({ queryKey: [REPORTS_QUERY_KEY] });
    },
  });

  const setPage = useCallback((page: number) => {
    setPageNum(page);
  }, []);

  return {
    records,
    total,
    totalPages,
    currentPage: pageNum,
    isLoading,
    isFetching,
    setPage,
    refetch,
    submitReview: submitReviewMutation,
    deleteRecord: deleteRecordMutation,
  };
}

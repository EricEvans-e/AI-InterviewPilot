import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { teacherService } from "@/services/teacherService";
import { buildInterviewReportViewModel } from "@/hooks/interview/report/interviewReportData.shared";

const QUERY_KEY = "teacher-interview-report";

export function useTeacherInterviewReport(sessionId: string | null) {
  const query = useQuery({
    queryKey: [QUERY_KEY, sessionId],
    enabled: Boolean(sessionId),
    queryFn: () => teacherService.getSessionReport(sessionId as string),
    retry: false,
    refetchOnWindowFocus: false,
    staleTime: 60_000,
  });

  const recordError = useMemo(() => {
    if (!query.error) return null;
    return query.error instanceof Error
      ? query.error.message
      : "加载面试报告时发生错误，请稍后重试。";
  }, [query.error]);

  const reportViewModel = useMemo(
    () => buildInterviewReportViewModel(query.data ?? null),
    [query.data],
  );

  return {
    isRecordLoading: query.isLoading || query.isFetching,
    recordError,
    record: query.data ?? null,
    ...reportViewModel,
  };
}

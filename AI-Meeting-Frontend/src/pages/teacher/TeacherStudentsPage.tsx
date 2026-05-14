import { useState, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  User,
  GraduationCap,
  Building2,
  BookOpen,
  Calendar,
  FileText,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";
import { useStudentReports } from "@/hooks/teacher/useStudentReports";
import StudentReportList from "@/components/teacher/StudentReportList";
import TeacherReviewForm from "@/components/teacher/TeacherReviewForm";
import type { StudentReportDTO, ReviewSubmitDTO } from "@/services/teacherService";

export default function TeacherStudentsPage() {
  const {
    reports,
    total,
    totalPages,
    currentPage,
    isLoading,
    isFetching,
    setPage,
    updateFilters,
    submitReview,
  } = useStudentReports();

  const [selectedReport, setSelectedReport] = useState<StudentReportDTO | null>(
    null,
  );

  const handleSelect = useCallback((report: StudentReportDTO) => {
    setSelectedReport((prev) => (prev?.id === report.id ? null : report));
  }, []);

  const handleFilterChange = useCallback(
    (filters: { collegeId?: number; majorId?: string; status?: string }) => {
      updateFilters(filters);
    },
    [updateFilters],
  );

  const handleReviewSubmit = useCallback(
    (data: ReviewSubmitDTO) => {
      submitReview.mutate(data, {
        onSuccess: () => {
          window.alert("点评提交成功");
        },
        onError: () => {
          window.alert("点评提交失败，请重试");
        },
      });
    },
    [submitReview],
  );

  const formatTime = (time?: string) => {
    if (!time) return "-";
    try {
      return new Date(time).toLocaleString("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return time;
    }
  };

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">学生报告</h1>
          <p className="mt-0.5 text-sm text-slate-500">
            查看学生面试报告并提供教师点评
          </p>
        </div>
      </div>

      {/* Main content: left list + right detail */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left: Report list */}
        <div
          className={cn(
            "flex flex-col overflow-hidden border-r border-slate-200 transition-all",
            selectedReport ? "w-[55%]" : "w-full",
          )}
        >
          <StudentReportList
            reports={reports}
            total={total}
            currentPage={currentPage}
            totalPages={totalPages}
            isLoading={isLoading || isFetching}
            selectedId={selectedReport?.id ?? null}
            onPageChange={setPage}
            onSelect={handleSelect}
            onFilterChange={handleFilterChange}
          />
        </div>

        {/* Right: Detail panel */}
        <AnimatePresence mode="wait">
          {selectedReport ? (
            <motion.div
              key={selectedReport.id}
              className="w-[45%] overflow-hidden"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }}
            >
              <ScrollArea className="h-full">
                <div className="space-y-4 p-5">
                  {/* Report info card */}
                  <Card className="border-slate-100">
                    <CardHeader className="pb-3">
                      <CardTitle className="flex items-center gap-2 text-base font-semibold text-slate-800">
                        <FileText className="h-4.5 w-4.5 text-indigo-500" />
                        报告详情
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="grid grid-cols-2 gap-4">
                        <InfoRow
                          icon={<User className="h-4 w-4" />}
                          label="学生姓名"
                          value={selectedReport.studentName}
                        />
                        <InfoRow
                          icon={<GraduationCap className="h-4 w-4" />}
                          label="会话标题"
                          value={selectedReport.sessionTitle}
                        />
                        <InfoRow
                          icon={<Building2 className="h-4 w-4" />}
                          label="院校"
                          value={selectedReport.collegeName}
                        />
                        <InfoRow
                          icon={<BookOpen className="h-4 w-4" />}
                          label="专业"
                          value={selectedReport.majorName}
                        />
                        <InfoRow
                          icon={<Calendar className="h-4 w-4" />}
                          label="创建时间"
                          value={formatTime(selectedReport.createTime)}
                        />
                      </div>

                      <Separator className="my-4" />

                      {/* Score highlight */}
                      <div className="flex items-center justify-center gap-6 rounded-lg bg-slate-50 p-4">
                        <div className="text-center">
                          <p className="text-xs text-slate-500">AI 综合评分</p>
                          <p className="mt-1 text-3xl font-bold text-indigo-600">
                            {selectedReport.overallScore ?? "--"}
                          </p>
                        </div>
                        <div className="h-10 w-px bg-slate-200" />
                        <div className="text-center">
                          <p className="text-xs text-slate-500">状态</p>
                          <p className="mt-1 text-sm font-medium text-slate-700">
                            {selectedReport.status === "completed"
                              ? "已完成"
                              : selectedReport.status === "in_progress"
                                ? "进行中"
                                : selectedReport.status ?? "-"}
                          </p>
                        </div>
                      </div>
                    </CardContent>
                  </Card>

                  {/* Q&A summary placeholder */}
                  <Card className="border-slate-100">
                    <CardHeader className="pb-3">
                      <CardTitle className="text-base font-semibold text-slate-800">
                        问答回放摘要
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="rounded-lg bg-slate-50 p-4 text-center text-sm text-slate-400">
                        详细的问答回放数据请前往学生端报告页面查看
                      </div>
                    </CardContent>
                  </Card>

                  {/* Review form */}
                  <TeacherReviewForm
                    reportId={selectedReport.id}
                    isSubmitting={submitReview.isPending}
                    onSubmit={handleReviewSubmit}
                  />
                </div>
              </ScrollArea>
            </motion.div>
          ) : null}
        </AnimatePresence>
      </div>
    </div>
  );
}

// ── Helper component ──

function InfoRow({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value?: string;
}) {
  return (
    <div className="flex items-start gap-2.5">
      <div className="mt-0.5 text-slate-400">{icon}</div>
      <div className="min-w-0">
        <p className="text-xs text-slate-500">{label}</p>
        <p className="mt-0.5 truncate text-sm font-medium text-slate-800">
          {value ?? "-"}
        </p>
      </div>
    </div>
  );
}

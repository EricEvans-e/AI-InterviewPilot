import { useState, useEffect, useCallback } from "react";
import { Search, X, Eye } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { teacherService } from "@/services/teacherService";
import type { StudentReportDTO } from "@/services/teacherService";
import type { CollegeRespDTO, MajorRespDTO } from "@/services/questionBankService";

interface StudentReportListProps {
  reports: StudentReportDTO[];
  total: number;
  currentPage: number;
  totalPages: number;
  isLoading: boolean;
  selectedId: number | null;
  onPageChange: (page: number) => void;
  onSelect: (report: StudentReportDTO) => void;
  onFilterChange: (filters: {
    collegeId?: number;
    majorId?: string;
    status?: string;
  }) => void;
}

const STATUS_MAP: Record<string, { label: string; className: string }> = {
  completed: { label: "已完成", className: "bg-green-100 text-green-700" },
  in_progress: { label: "进行中", className: "bg-blue-100 text-blue-700" },
  pending: { label: "待开始", className: "bg-slate-100 text-slate-600" },
  cancelled: { label: "已取消", className: "bg-red-100 text-red-700" },
};

export default function StudentReportList({
  reports,
  total,
  currentPage,
  totalPages,
  isLoading,
  selectedId,
  onPageChange,
  onSelect,
  onFilterChange,
}: StudentReportListProps) {
  const [colleges, setColleges] = useState<CollegeRespDTO[]>([]);
  const [majors, setMajors] = useState<MajorRespDTO[]>([]);
  const [collegeId, setCollegeId] = useState<number | "">("");
  const [majorId, setMajorId] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  // Load colleges on mount
  useEffect(() => {
    teacherService.listColleges().then(setColleges).catch(() => {});
  }, []);

  // Load majors when college changes
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
    const numVal = value ? Number(value) : "";
    setCollegeId(numVal);
    setMajors([]);
    setMajorId("");
  }, []);

  const handleApplyFilters = useCallback(() => {
    onFilterChange({
      collegeId: collegeId ? (collegeId as number) : undefined,
      majorId: majorId || undefined,
      status: statusFilter || undefined,
    });
  }, [collegeId, majorId, statusFilter, onFilterChange]);

  const handleClearFilters = useCallback(() => {
    setCollegeId("");
    setMajors([]);
    setMajorId("");
    setStatusFilter("");
    onFilterChange({
      collegeId: undefined,
      majorId: undefined,
      status: undefined,
    });
  }, [onFilterChange]);

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
      {/* Filters */}
      <div className="border-b border-slate-200 bg-slate-50/50 px-4 py-3">
        <div className="flex flex-wrap items-end gap-2">
          <div className="space-y-1">
            <label className="text-xs text-slate-500">院校</label>
            <select
              value={collegeId}
              onChange={(e) => handleCollegeChange(e.target.value)}
              className="flex h-8 rounded-md border border-input bg-white px-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
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
              onChange={(e) => setMajorId(e.target.value)}
              className="flex h-8 rounded-md border border-input bg-white px-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
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
            <label className="text-xs text-slate-500">状态</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="flex h-8 rounded-md border border-input bg-white px-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">全部</option>
              <option value="completed">已完成</option>
              <option value="in_progress">进行中</option>
              <option value="pending">待开始</option>
              <option value="cancelled">已取消</option>
            </select>
          </div>
          <div className="flex items-end gap-1.5">
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

      {/* Table */}
      <div className="flex-1 overflow-auto">
        <div className="rounded-lg border border-slate-200 bg-white">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-left">
                  <th className="px-4 py-3 font-medium text-slate-600">学生姓名</th>
                  <th className="px-4 py-3 font-medium text-slate-600">会话标题</th>
                  <th className="px-4 py-3 font-medium text-slate-600">院校</th>
                  <th className="px-4 py-3 font-medium text-slate-600">专业</th>
                  <th className="px-4 py-3 font-medium text-slate-600">总分</th>
                  <th className="px-4 py-3 font-medium text-slate-600">状态</th>
                  <th className="px-4 py-3 font-medium text-slate-600">创建时间</th>
                  <th className="px-4 py-3 font-medium text-slate-600">操作</th>
                </tr>
              </thead>
              <tbody>
                {isLoading ? (
                  <tr>
                    <td colSpan={8} className="px-4 py-12 text-center text-slate-400">
                      加载中...
                    </td>
                  </tr>
                ) : reports.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="px-4 py-12 text-center text-slate-400">
                      暂无学生报告数据
                    </td>
                  </tr>
                ) : (
                  reports.map((report) => {
                    const status = STATUS_MAP[report.status ?? ""] ?? {
                      label: report.status ?? "-",
                      className: "bg-slate-100 text-slate-600",
                    };
                    const isSelected = selectedId === report.id;
                    return (
                      <tr
                        key={report.id}
                        className={cn(
                          "border-b border-slate-100 last:border-0 cursor-pointer transition-colors",
                          isSelected
                            ? "bg-indigo-50/60"
                            : "hover:bg-slate-50/50",
                        )}
                        onClick={() => onSelect(report)}
                      >
                        <td className="px-4 py-3 font-medium text-slate-800">
                          {report.studentName ?? "-"}
                        </td>
                        <td className="max-w-[180px] truncate px-4 py-3 text-slate-600">
                          {report.sessionTitle ?? "-"}
                        </td>
                        <td className="px-4 py-3 text-slate-600">
                          {report.collegeName ?? "-"}
                        </td>
                        <td className="px-4 py-3 text-slate-600">
                          {report.majorName ?? "-"}
                        </td>
                        <td className="px-4 py-3">
                          <span className="font-semibold text-indigo-600">
                            {report.overallScore ?? "-"}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={cn(
                              "inline-block rounded-full px-2 py-0.5 text-xs font-medium",
                              status.className,
                            )}
                          >
                            {status.label}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-slate-500">
                          {formatTime(report.createTime)}
                        </td>
                        <td className="px-4 py-3">
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-7 gap-1 text-xs"
                            onClick={(e) => {
                              e.stopPropagation();
                              onSelect(report);
                            }}
                          >
                            <Eye className="h-3.5 w-3.5" />
                            查看
                          </Button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {total > 0 && (
            <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3">
              <span className="text-sm text-slate-500">共 {total} 条记录</span>
              <div className="flex items-center gap-1">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage <= 1}
                  onClick={() => onPageChange(currentPage - 1)}
                >
                  上一页
                </Button>
                <span className="mx-2 text-sm text-slate-600">
                  {currentPage} / {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage >= totalPages}
                  onClick={() => onPageChange(currentPage + 1)}
                >
                  下一页
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

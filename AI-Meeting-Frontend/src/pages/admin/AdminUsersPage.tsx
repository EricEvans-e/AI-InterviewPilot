import { useState, useCallback } from "react";
import { Search, Shield, ChevronLeft, ChevronRight } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useAdminUsers } from "@/hooks/admin/useAdminUsers";
import type { UserPageRespDTO } from "@/services/adminService";

const STATUS_OPTIONS = [
  { label: "全部", value: undefined },
  { label: "正常", value: 0 },
  { label: "禁用", value: 1 },
] as const;

export default function AdminUsersPage() {
  const {
    users,
    total,
    totalPages,
    currentPage,
    filters,
    isLoading,
    isFetching,
    setPage,
    updateFilters,
    addAdmin,
  } = useAdminUsers();

  const [keyword, setKeyword] = useState("");

  const handleSearch = useCallback(() => {
    updateFilters({ keyword: keyword.trim() || undefined });
  }, [keyword, updateFilters]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Enter") {
        handleSearch();
      }
    },
    [handleSearch],
  );

  const handleStatusChange = useCallback(
    (status: number | undefined) => {
      updateFilters({ status });
    },
    [updateFilters],
  );

  const handleAddAdmin = useCallback(
    (user: UserPageRespDTO) => {
      if (user.role === "admin") {
        window.alert("该用户已是管理员");
        return;
      }
      if (window.confirm(`确认将用户 "${user.username}" 设为管理员？`)) {
        addAdmin.mutate(user.username, {
          onSuccess: () => {
            window.alert("已成功设为管理员");
          },
          onError: () => {
            window.alert("操作失败，请重试");
          },
        });
      }
    },
    [addAdmin],
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
      <div className="border-b border-slate-200 px-6 py-4">
        <h1 className="text-xl font-semibold text-slate-800">用户管理</h1>
        <p className="mt-0.5 text-sm text-slate-500">
          查看和管理平台注册用户
        </p>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 border-b border-slate-100 px-6 py-3">
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              placeholder="搜索用户名或姓名"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onKeyDown={handleKeyDown}
              className="h-9 w-56 pl-8 text-sm"
            />
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={handleSearch}
            disabled={isFetching}
          >
            搜索
          </Button>
        </div>

        <div className="flex items-center gap-1">
          {STATUS_OPTIONS.map((opt) => {
            const isActive =
              opt.value === undefined
                ? filters.status === undefined
                : filters.status === opt.value;
            return (
              <Button
                key={opt.label}
                variant={isActive ? "secondary" : "ghost"}
                size="sm"
                className={cn("text-xs", isActive && "font-medium")}
                onClick={() => handleStatusChange(opt.value)}
              >
                {opt.label}
              </Button>
            );
          })}
        </div>
      </div>

      {/* Table */}
      <div className="flex-1 overflow-auto p-6">
        <Card className="border-slate-100">
          <CardContent className="p-0">
            {isLoading ? (
              <div className="flex h-48 items-center justify-center text-sm text-slate-400">
                加载中...
              </div>
            ) : users.length === 0 ? (
              <div className="flex h-48 items-center justify-center text-sm text-slate-400">
                暂无数据
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 bg-slate-50 text-left">
                      <th className="px-4 py-3 font-medium text-slate-600">ID</th>
                      <th className="px-4 py-3 font-medium text-slate-600">用户名</th>
                      <th className="px-4 py-3 font-medium text-slate-600">真实姓名</th>
                      <th className="px-4 py-3 font-medium text-slate-600">手机号</th>
                      <th className="px-4 py-3 font-medium text-slate-600">邮箱</th>
                      <th className="px-4 py-3 font-medium text-slate-600">状态</th>
                      <th className="px-4 py-3 font-medium text-slate-600">管理员</th>
                      <th className="px-4 py-3 font-medium text-slate-600">注册时间</th>
                      <th className="px-4 py-3 font-medium text-slate-600">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((user) => (
                      <tr
                        key={user.id}
                        className="border-b border-slate-50 transition-colors hover:bg-slate-50/50"
                      >
                        <td className="px-4 py-3 text-slate-500">{user.id}</td>
                        <td className="px-4 py-3 font-medium text-slate-800">
                          {user.username}
                        </td>
                        <td className="px-4 py-3 text-slate-600">
                          {user.realName ?? "-"}
                        </td>
                        <td className="px-4 py-3 text-slate-600">
                          {user.phone ?? "-"}
                        </td>
                        <td className="px-4 py-3 text-slate-600">
                          {user.mail ?? "-"}
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={cn(
                              "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
                              user.delFlag === 1
                                ? "bg-red-50 text-red-600"
                                : "bg-green-50 text-green-600",
                            )}
                          >
                            {user.delFlag === 1 ? "禁用" : "正常"}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span
                            className={cn(
                              "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
                              user.role === "admin"
                                ? "bg-indigo-50 text-indigo-600"
                                : "bg-slate-100 text-slate-500",
                            )}
                          >
                            {user.role === "admin" ? "是" : "否"}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-slate-500">
                          {formatTime(user.createTime)}
                        </td>
                        <td className="px-4 py-3">
                          {user.role !== "admin" && (
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-7 gap-1 text-xs text-indigo-600 hover:text-indigo-700"
                              onClick={() => handleAddAdmin(user)}
                              disabled={addAdmin.isPending}
                            >
                              <Shield className="h-3.5 w-3.5" />
                              设为管理员
                            </Button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="mt-4 flex items-center justify-between">
            <p className="text-sm text-slate-500">
              共 {total} 条记录，第 {currentPage}/{totalPages} 页
            </p>
            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(currentPage - 1)}
                disabled={currentPage <= 1 || isFetching}
              >
                <ChevronLeft className="h-4 w-4" />
                上一页
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(currentPage + 1)}
                disabled={currentPage >= totalPages || isFetching}
              >
                下一页
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

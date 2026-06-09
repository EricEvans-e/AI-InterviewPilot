import { useState, useEffect, useCallback } from "react";
import { Plus, Pencil, Trash2, Power, PowerOff, Star } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";
import { teacherService } from "@/services/teacherService";
import type {
  AiPropertiesDTO,
  AiPropertiesCreateDTO,
} from "@/services/teacherService";

const AI_TYPE_OPTIONS = [
  { value: "openai", label: "Mimo OpenAI Compatible" },
];

const PAGE_SIZE = 10;

export default function TeacherAiConfigPage() {
  const [records, setRecords] = useState<AiPropertiesDTO[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [isLoading, setIsLoading] = useState(false);

  // Filter
  const [filterName, setFilterName] = useState("");
  const [filterType, setFilterType] = useState("");

  // Dialog
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<AiPropertiesDTO | null>(null);
  const [form, setForm] = useState<AiPropertiesCreateDTO>(defaultForm());
  const [isSaving, setIsSaving] = useState(false);

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    try {
      const resp = await teacherService.pageAiProperties({
        current: currentPage,
        size: PAGE_SIZE,
        aiName: filterName || undefined,
        aiType: filterType || undefined,
      });
      setRecords(resp.records ?? []);
      setTotal(resp.total ?? 0);
    } catch {
      setRecords([]);
      setTotal(0);
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, filterName, filterType]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  function defaultForm(): AiPropertiesCreateDTO {
    return {
      aiName: "",
      aiType: "openai",
      apiKey: "",
      apiUrl: "https://token-plan-cn.xiaomimimo.com/v1",
      modelName: "",
      maxTokens: 4096,
      temperature: 0.7,
      systemPrompt: "",
      isEnabled: 1,
    };
  }

  function openCreate() {
    setEditing(null);
    setForm(defaultForm());
    setDialogOpen(true);
  }

  function openEdit(item: AiPropertiesDTO) {
    setEditing(item);
    setForm({
      aiName: item.aiName,
      aiType: item.aiType,
      apiKey: "", // don't pre-fill masked key
      apiUrl: item.apiUrl,
      modelName: item.modelName,
      maxTokens: item.maxTokens,
      temperature: item.temperature,
      systemPrompt: item.systemPrompt,
      isEnabled: item.isEnabled,
      isDefault: item.isDefault,
    });
    setDialogOpen(true);
  }

  async function handleSubmit() {
    if (!form.aiName.trim() || !form.aiType || !form.apiUrl.trim() || !form.modelName.trim()) {
      window.alert("请填写必填字段：名称、类型、API地址、模型名称");
      return;
    }
    setIsSaving(true);
    try {
      if (editing) {
        await teacherService.updateAiProperties({ ...form, id: editing.id });
        window.alert("更新成功");
      } else {
        await teacherService.createAiProperties(form);
        window.alert("创建成功");
      }
      setDialogOpen(false);
      fetchData();
    } catch {
      window.alert(editing ? "更新失败" : "创建失败");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleDelete(item: AiPropertiesDTO) {
    if (!window.confirm(`确定删除「${item.aiName}」吗？`)) return;
    try {
      await teacherService.deleteAiProperties(item.id);
      fetchData();
    } catch {
      window.alert("删除失败");
    }
  }

  async function handleToggleStatus(item: AiPropertiesDTO) {
    const newStatus = item.isEnabled === 1 ? 0 : 1;
    try {
      await teacherService.toggleAiPropertiesStatus(item.id, newStatus);
      fetchData();
    } catch {
      window.alert("状态切换失败");
    }
  }

  async function handleSetDefault(item: AiPropertiesDTO) {
    try {
      await teacherService.setDefaultAiProperties(item.id);
      fetchData();
    } catch {
      window.alert("设置默认失败");
    }
  }

  function handleFilter() {
    setCurrentPage(1);
    fetchData();
  }

  function handleClearFilter() {
    setFilterName("");
    setFilterType("");
    setCurrentPage(1);
  }

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4">
        <div>
          <h1 className="text-xl font-semibold text-slate-800">AI 模型配置</h1>
          <p className="mt-0.5 text-sm text-slate-500">
            管理 AI 对话可用的模型及其参数配置
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="mr-1.5 h-4 w-4" />
          新增配置
        </Button>
      </div>

      {/* Filters */}
      <div className="border-b border-slate-200 bg-slate-50/50 px-6 py-3">
        <div className="flex flex-wrap items-end gap-3">
          <div className="space-y-1">
            <label className="text-xs text-slate-500">名称</label>
            <Input
              value={filterName}
              onChange={(e) => setFilterName(e.target.value)}
              placeholder="搜索名称"
              className="h-9 w-40"
            />
          </div>
          <div className="space-y-1">
            <label className="text-xs text-slate-500">类型</label>
            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
              className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <option value="">全部</option>
              {AI_TYPE_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>
          <Button size="sm" onClick={handleFilter}>
            筛选
          </Button>
          <Button variant="ghost" size="sm" onClick={handleClearFilter}>
            清除
          </Button>
        </div>
      </div>

      {/* Table */}
      <div className="flex-1 overflow-auto p-6">
        <div className="rounded-lg border border-slate-200 bg-white">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-left">
                  <th className="px-4 py-3 font-medium text-slate-600">ID</th>
                  <th className="px-4 py-3 font-medium text-slate-600">名称</th>
                  <th className="px-4 py-3 font-medium text-slate-600">类型</th>
                  <th className="px-4 py-3 font-medium text-slate-600">模型</th>
                  <th className="px-4 py-3 font-medium text-slate-600">API 地址</th>
                  <th className="px-4 py-3 font-medium text-slate-600">API Key</th>
                  <th className="px-4 py-3 font-medium text-slate-600">Max Tokens</th>
                  <th className="px-4 py-3 font-medium text-slate-600">温度</th>
                  <th className="px-4 py-3 font-medium text-slate-600">状态</th>
                  <th className="px-4 py-3 font-medium text-slate-600">操作</th>
                </tr>
              </thead>
              <tbody>
                {isLoading ? (
                  <tr>
                    <td colSpan={10} className="px-4 py-12 text-center text-slate-400">
                      加载中...
                    </td>
                  </tr>
                ) : records.length === 0 ? (
                  <tr>
                    <td colSpan={10} className="px-4 py-12 text-center text-slate-400">
                      暂无 AI 模型配置
                    </td>
                  </tr>
                ) : (
                  records.map((item) => (
                    <tr
                      key={item.id}
                      className="border-b border-slate-100 last:border-0 hover:bg-slate-50/50"
                    >
                      <td className="px-4 py-3 text-slate-500">{item.id}</td>
                      <td className="max-w-[160px] truncate px-4 py-3 font-medium text-slate-800">
                        {item.aiName}
                        {item.isDefault === 1 && (
                          <Star className="ml-1 inline h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                        )}
                      </td>
                      <td className="px-4 py-3 text-slate-600">
                        <span className="inline-block rounded-full bg-violet-100 px-2 py-0.5 text-xs font-medium text-violet-700">
                          {AI_TYPE_OPTIONS.find((o) => o.value === item.aiType)?.label ?? item.aiType}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-slate-600">{item.modelName}</td>
                      <td className="max-w-[200px] truncate px-4 py-3 text-xs text-slate-500">
                        {item.apiUrl}
                      </td>
                      <td className="px-4 py-3 text-xs text-slate-400">
                        {item.apiKey || "-"}
                      </td>
                      <td className="px-4 py-3 text-slate-600">{item.maxTokens ?? "-"}</td>
                      <td className="px-4 py-3 text-slate-600">{item.temperature ?? "-"}</td>
                      <td className="px-4 py-3">
                        <span
                          className={cn(
                            "inline-block rounded-full px-2 py-0.5 text-xs font-medium",
                            item.isEnabled === 1
                              ? "bg-green-100 text-green-700"
                              : "bg-slate-100 text-slate-600",
                          )}
                        >
                          {item.isEnabled === 1 ? "启用" : "禁用"}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1">
                          {item.isDefault !== 1 && (
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              onClick={() => handleSetDefault(item)}
                              title="设为默认"
                            >
                              <Star className="h-3.5 w-3.5 text-slate-400" />
                            </Button>
                          )}
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => handleToggleStatus(item)}
                            title={item.isEnabled === 1 ? "禁用" : "启用"}
                          >
                            {item.isEnabled === 1 ? (
                              <PowerOff className="h-3.5 w-3.5 text-slate-400" />
                            ) : (
                              <Power className="h-3.5 w-3.5 text-green-500" />
                            )}
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => openEdit(item)}
                            title="编辑"
                          >
                            <Pencil className="h-3.5 w-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-red-500 hover:text-red-700"
                            onClick={() => handleDelete(item)}
                            title="删除"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))
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
                  onClick={() => setCurrentPage((p) => p - 1)}
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
                  onClick={() => setCurrentPage((p) => p + 1)}
                >
                  下一页
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Create / Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editing ? "编辑 AI 模型配置" : "新增 AI 模型配置"}
            </DialogTitle>
          </DialogHeader>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>
                名称 <span className="text-red-500">*</span>
              </Label>
              <Input
                value={form.aiName}
                onChange={(e) => setForm({ ...form, aiName: e.target.value })}
                placeholder="如：Mimo V2.5"
              />
            </div>
            <div className="space-y-1.5">
              <Label>
                类型 <span className="text-red-500">*</span>
              </Label>
              <select
                value={form.aiType}
                onChange={(e) => setForm({ ...form, aiType: e.target.value })}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <option value="">请选择</option>
                {AI_TYPE_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>
                模型名称 <span className="text-red-500">*</span>
              </Label>
              <Input
                value={form.modelName}
                onChange={(e) => setForm({ ...form, modelName: e.target.value })}
                placeholder="如：mimo-v2.5"
              />
            </div>
            <div className="space-y-1.5">
              <Label>
                API 地址 <span className="text-red-500">*</span>
              </Label>
              <Input
                value={form.apiUrl}
                onChange={(e) => setForm({ ...form, apiUrl: e.target.value })}
                placeholder="https://token-plan-cn.xiaomimimo.com/v1"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label>API Key</Label>
            <Input
              type="password"
              value={form.apiKey}
              onChange={(e) => setForm({ ...form, apiKey: e.target.value })}
              placeholder={editing ? "留空则不修改" : "请输入 API Key"}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>API Secret</Label>
              <Input
                type="password"
                value={form.apiSecret ?? ""}
                onChange={(e) => setForm({ ...form, apiSecret: e.target.value || undefined })}
                placeholder="部分 AI 需要"
              />
            </div>
            <div className="space-y-1.5">
              <Label>Project ID</Label>
              <Input
                value={form.projectId ?? ""}
                onChange={(e) => setForm({ ...form, projectId: e.target.value || undefined })}
                placeholder="OpenAI 等需要"
              />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className="space-y-1.5">
              <Label>Max Tokens</Label>
              <Input
                type="number"
                value={form.maxTokens ?? ""}
                onChange={(e) =>
                  setForm({ ...form, maxTokens: Number(e.target.value) || undefined })
                }
              />
            </div>
            <div className="space-y-1.5">
              <Label>温度 (0-2)</Label>
              <Input
                type="number"
                step="0.1"
                min={0}
                max={2}
                value={form.temperature ?? ""}
                onChange={(e) =>
                  setForm({ ...form, temperature: Number(e.target.value) || undefined })
                }
              />
            </div>
            <div className="space-y-1.5">
              <Label>启用状态</Label>
              <select
                value={form.isEnabled ?? 1}
                onChange={(e) => setForm({ ...form, isEnabled: Number(e.target.value) })}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <option value={1}>启用</option>
                <option value={0}>禁用</option>
              </select>
            </div>
            <div className="space-y-1.5">
              <Label>设为默认</Label>
              <select
                value={form.isDefault ?? 0}
                onChange={(e) => setForm({ ...form, isDefault: Number(e.target.value) })}
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                <option value={0}>否</option>
                <option value={1}>是（同类型优先使用）</option>
              </select>
            </div>
          </div>

          <div className="space-y-1.5">
            <Label>系统提示词</Label>
            <Textarea
              value={form.systemPrompt ?? ""}
              onChange={(e) => setForm({ ...form, systemPrompt: e.target.value })}
              placeholder="可选，设置模型的系统提示词"
              rows={4}
            />
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={isSaving}>
              取消
            </Button>
            <Button onClick={handleSubmit} disabled={isSaving}>
              {isSaving ? "保存中..." : editing ? "更新" : "创建"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

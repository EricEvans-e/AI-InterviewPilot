import { useState, useCallback, useEffect } from "react";
import { Plus, Pencil, Trash2, Search, X } from "lucide-react";
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
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";
import {
  useColleges,
  useMajors,
  useExamOutlines,
} from "@/hooks/teacher/useCollegeManage";
import { collegeService } from "@/services/collegeService";
import type {
  CollegeCreateDTO,
  MajorCreateDTO,
  ExamOutlineCreateDTO,
} from "@/services/collegeService";
import type {
  CollegeRespDTO,
  MajorRespDTO,
} from "@/services/questionBankService";
import type { ExamOutlineRespDTO } from "@/services/collegeService";

// ── Tab definitions ──

type TabKey = "colleges" | "majors" | "examOutlines";

const TABS: { key: TabKey; label: string }[] = [
  { key: "colleges", label: "院校管理" },
  { key: "majors", label: "专业管理" },
  { key: "examOutlines", label: "考纲管理" },
];

// ── Reusable pagination ──

function Pagination({
  total,
  currentPage,
  totalPages,
  onPageChange,
}: {
  total: number;
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  if (total <= 0) return null;
  return (
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
  );
}

// ════════════════════════════════════════════════════════════════════
// COLLEGES TAB
// ════════════════════════════════════════════════════════════════════

function CollegeFormDialog({
  open,
  onOpenChange,
  onSubmit,
  isSubmitting,
  editing,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: CollegeCreateDTO) => void;
  isSubmitting: boolean;
  editing: CollegeRespDTO | null;
}) {
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [type, setType] = useState("");
  const [province, setProvince] = useState("");
  const [city, setCity] = useState("");
  const [level, setLevel] = useState("");
  const [officialUrl, setOfficialUrl] = useState("");
  const [remark, setRemark] = useState("");

  useEffect(() => {
    if (!open) return;
    if (editing) {
      /* eslint-disable react-hooks/set-state-in-effect -- form init from props */
      setName(editing.name ?? "");
      setCode(editing.code ?? "");
      setType(editing.type ?? "");
      setProvince(editing.province ?? "");
      setCity(editing.city ?? "");
      setLevel(editing.level ?? "");
      setOfficialUrl(editing.officialUrl ?? "");
      setRemark(editing.remark ?? "");
      /* eslint-enable react-hooks/set-state-in-effect */
    } else {
      setName("");
      setCode("");
      setType("");
      setProvince("");
      setCity("");
      setLevel("");
      setOfficialUrl("");
      setRemark("");
    }
  }, [open, editing]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    onSubmit({
      name: name.trim(),
      code: code.trim() || undefined,
      type: type.trim() || undefined,
      province: province.trim() || undefined,
      city: city.trim() || undefined,
      level: level.trim() || undefined,
      officialUrl: officialUrl.trim() || undefined,
      remark: remark.trim() || undefined,
    });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>{editing ? "编辑院校" : "新建院校"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit}>
          <ScrollArea className="max-h-[60vh]">
            <div className="space-y-4 pr-4">
              <div className="space-y-1.5">
                <Label>
                  院校名称 <span className="text-red-500">*</span>
                </Label>
                <Input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="请输入院校名称"
                  required
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label>院校代码</Label>
                  <Input
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    placeholder="例如：12345"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label>院校类型</Label>
                  <Input
                    value={type}
                    onChange={(e) => setType(e.target.value)}
                    placeholder="例如：综合"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label>省份</Label>
                  <Input
                    value={province}
                    onChange={(e) => setProvince(e.target.value)}
                    placeholder="例如：浙江"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label>城市</Label>
                  <Input
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="例如：杭州"
                  />
                </div>
              </div>
              <div className="space-y-1.5">
                <Label>办学层次</Label>
                <Input
                  value={level}
                  onChange={(e) => setLevel(e.target.value)}
                  placeholder="例如：高职"
                />
              </div>
              <div className="space-y-1.5">
                <Label>官网链接</Label>
                <Input
                  value={officialUrl}
                  onChange={(e) => setOfficialUrl(e.target.value)}
                  placeholder="https://..."
                />
              </div>
              <div className="space-y-1.5">
                <Label>备注</Label>
                <Textarea
                  value={remark}
                  onChange={(e) => setRemark(e.target.value)}
                  placeholder="备注信息"
                  rows={2}
                />
              </div>
            </div>
          </ScrollArea>
          <DialogFooter className="mt-6">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isSubmitting}
            >
              取消
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting || !name.trim()}
            >
              {isSubmitting
                ? "保存中..."
                : editing
                  ? "保存修改"
                  : "创建院校"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function CollegesTab() {
  const {
    colleges,
    total,
    totalPages,
    currentPage,
    isLoading,
    isFetching,
    setPage,
    updateFilters,
    createCollege,
    updateCollege,
    deleteCollege,
  } = useColleges();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<CollegeRespDTO | null>(null);
  const [searchName, setSearchName] = useState("");

  const handleApplySearch = useCallback(() => {
    updateFilters({ name: searchName.trim() || undefined });
  }, [searchName, updateFilters]);

  const handleClearSearch = useCallback(() => {
    setSearchName("");
    updateFilters({ name: undefined });
  }, [updateFilters]);

  const handleDelete = useCallback(
    (c: CollegeRespDTO) => {
      if (window.confirm(`确定删除院校「${c.name}」吗？`)) {
        deleteCollege.mutate(c.id, {
          onSuccess: () => window.alert("删除成功"),
          onError: () => window.alert("删除失败，请重试"),
        });
      }
    },
    [deleteCollege],
  );

  const handleSubmit = useCallback(
    (data: CollegeCreateDTO) => {
      if (editing) {
        updateCollege.mutate(
          { id: editing.id, data },
          {
            onSuccess: () => {
              setFormOpen(false);
              setEditing(null);
              window.alert("更新成功");
            },
            onError: () => window.alert("更新失败，请重试"),
          },
        );
      } else {
        createCollege.mutate(data, {
          onSuccess: () => {
            setFormOpen(false);
            window.alert("创建成功");
          },
          onError: () => window.alert("创建失败，请重试"),
        });
      }
    },
    [editing, createCollege, updateCollege],
  );

  const isSubmitting = createCollege.isPending || updateCollege.isPending;

  return (
    <div className="flex flex-col gap-4">
      {/* Toolbar */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Input
            value={searchName}
            onChange={(e) => setSearchName(e.target.value)}
            placeholder="搜索院校名称..."
            className="w-56"
            onKeyDown={(e) => {
              if (e.key === "Enter") handleApplySearch();
            }}
          />
          <Button size="sm" onClick={handleApplySearch}>
            <Search className="mr-1 h-3.5 w-3.5" />
            筛选
          </Button>
          <Button variant="ghost" size="sm" onClick={handleClearSearch}>
            <X className="mr-1 h-3.5 w-3.5" />
            清除
          </Button>
        </div>
        <Button
          onClick={() => {
            setEditing(null);
            setFormOpen(true);
          }}
        >
          <Plus className="mr-1.5 h-4 w-4" />
          新建院校
        </Button>
      </div>

      {/* Table */}
      <div className="rounded-lg border border-slate-200 bg-white">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-left">
                <th className="px-4 py-3 font-medium text-slate-600">ID</th>
                <th className="px-4 py-3 font-medium text-slate-600">名称</th>
                <th className="px-4 py-3 font-medium text-slate-600">代码</th>
                <th className="px-4 py-3 font-medium text-slate-600">类型</th>
                <th className="px-4 py-3 font-medium text-slate-600">省份</th>
                <th className="px-4 py-3 font-medium text-slate-600">城市</th>
                <th className="px-4 py-3 font-medium text-slate-600">层次</th>
                <th className="px-4 py-3 font-medium text-slate-600">操作</th>
              </tr>
            </thead>
            <tbody>
              {isLoading || isFetching ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-4 py-12 text-center text-slate-400"
                  >
                    加载中...
                  </td>
                </tr>
              ) : colleges.length === 0 ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-4 py-12 text-center text-slate-400"
                  >
                    暂无院校数据
                  </td>
                </tr>
              ) : (
                colleges.map((c) => (
                  <tr
                    key={c.id}
                    className="border-b border-slate-100 last:border-0 hover:bg-slate-50/50"
                  >
                    <td className="px-4 py-3 text-slate-500">{c.id}</td>
                    <td className="max-w-[200px] truncate px-4 py-3 font-medium text-slate-800">
                      {c.name}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {c.code ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {c.type ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {c.province ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {c.city ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {c.level ?? "-"}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8"
                          onClick={() => {
                            setEditing(c);
                            setFormOpen(true);
                          }}
                          title="编辑"
                        >
                          <Pencil className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-red-500 hover:text-red-700"
                          onClick={() => handleDelete(c)}
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
        <Pagination
          total={total}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      </div>

      <CollegeFormDialog
        open={formOpen}
        onOpenChange={(open) => {
          setFormOpen(open);
          if (!open) setEditing(null);
        }}
        onSubmit={handleSubmit}
        isSubmitting={isSubmitting}
        editing={editing}
      />
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// MAJORS TAB
// ════════════════════════════════════════════════════════════════════

function MajorFormDialog({
  open,
  onOpenChange,
  onSubmit,
  isSubmitting,
  editing,
  colleges,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: MajorCreateDTO) => void;
  isSubmitting: boolean;
  editing: MajorRespDTO | null;
  colleges: CollegeRespDTO[];
}) {
  const [collegeId, setCollegeId] = useState<number | "">("");
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [category, setCategory] = useState("");
  const [targetType, setTargetType] = useState("");
  const [testForm, setTestForm] = useState("");
  const [testContent, setTestContent] = useState("");
  const [scoreStructure, setScoreStructure] = useState("");
  const [year, setYear] = useState<number | "">("");
  const [officialUrl, setOfficialUrl] = useState("");

  useEffect(() => {
    if (!open) return;
    if (editing) {
      /* eslint-disable react-hooks/set-state-in-effect -- form init from props */
      setCollegeId(editing.collegeId ?? "");
      setName(editing.name ?? "");
      setCode(editing.code ?? "");
      setCategory(editing.category ?? "");
      setTargetType(editing.targetType ?? "");
      setTestForm(editing.testForm ?? "");
      setTestContent(editing.testContent ?? "");
      setScoreStructure(editing.scoreStructure ?? "");
      setYear(editing.year ?? "");
      setOfficialUrl(editing.officialUrl ?? "");
      /* eslint-enable react-hooks/set-state-in-effect */
    } else {
      setCollegeId("");
      setName("");
      setCode("");
      setCategory("");
      setTargetType("");
      setTestForm("");
      setTestContent("");
      setScoreStructure("");
      setYear("");
      setOfficialUrl("");
    }
  }, [open, editing]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim() || !collegeId) return;
    onSubmit({
      collegeId: collegeId as number,
      name: name.trim(),
      code: code.trim() || undefined,
      category: category.trim() || undefined,
      targetType: targetType.trim() || undefined,
      testForm: testForm.trim() || undefined,
      testContent: testContent.trim() || undefined,
      scoreStructure: scoreStructure.trim() || undefined,
      year: year ? (year as number) : undefined,
      officialUrl: officialUrl.trim() || undefined,
    });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{editing ? "编辑专业" : "新建专业"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit}>
          <ScrollArea className="max-h-[60vh]">
            <div className="space-y-4 pr-4">
              <div className="space-y-1.5">
                <Label>
                  所属院校 <span className="text-red-500">*</span>
                </Label>
                <select
                  value={collegeId}
                  onChange={(e) =>
                    setCollegeId(
                      e.target.value ? Number(e.target.value) : "",
                    )
                  }
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  required
                >
                  <option value="">请选择院校</option>
                  {colleges.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label>
                    专业名称 <span className="text-red-500">*</span>
                  </Label>
                  <Input
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="请输入专业名称"
                    required
                  />
                </div>
                <div className="space-y-1.5">
                  <Label>专业代码</Label>
                  <Input
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    placeholder="例如：510201"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label>专业类别</Label>
                  <Input
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    placeholder="例如：计算机类"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label>招生对象</Label>
                  <Input
                    value={targetType}
                    onChange={(e) => setTargetType(e.target.value)}
                    placeholder="例如：普高"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label>考试形式</Label>
                  <Input
                    value={testForm}
                    onChange={(e) => setTestForm(e.target.value)}
                    placeholder="例如：笔试+面试"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label>年份</Label>
                  <Input
                    type="number"
                    value={year}
                    onChange={(e) =>
                      setYear(
                        e.target.value ? Number(e.target.value) : "",
                      )
                    }
                    placeholder="例如：2026"
                  />
                </div>
              </div>
              <div className="space-y-1.5">
                <Label>考试内容</Label>
                <Textarea
                  value={testContent}
                  onChange={(e) => setTestContent(e.target.value)}
                  placeholder="考试内容描述"
                  rows={2}
                />
              </div>
              <div className="space-y-1.5">
                <Label>分值结构</Label>
                <Textarea
                  value={scoreStructure}
                  onChange={(e) => setScoreStructure(e.target.value)}
                  placeholder="分值结构描述"
                  rows={2}
                />
              </div>
              <div className="space-y-1.5">
                <Label>官网链接</Label>
                <Input
                  value={officialUrl}
                  onChange={(e) => setOfficialUrl(e.target.value)}
                  placeholder="https://..."
                />
              </div>
            </div>
          </ScrollArea>
          <DialogFooter className="mt-6">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isSubmitting}
            >
              取消
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting || !name.trim() || !collegeId}
            >
              {isSubmitting
                ? "保存中..."
                : editing
                  ? "保存修改"
                  : "创建专业"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function MajorsTab() {
  const {
    majors,
    total,
    totalPages,
    currentPage,
    isLoading,
    isFetching,
    setPage,
    updateFilters,
    createMajor,
    updateMajor,
    deleteMajor,
  } = useMajors();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<MajorRespDTO | null>(null);
  const [colleges, setColleges] = useState<CollegeRespDTO[]>([]);
  const [filterCollegeId, setFilterCollegeId] = useState<number | "">("");
  const [searchName, setSearchName] = useState("");

  // Load colleges for filter and form selects
  useEffect(() => {
    collegeService.listColleges().then(setColleges).catch(() => {});
  }, []);

  const collegeNameMap = new Map(colleges.map((c) => [c.id, c.name]));

  const handleApplyFilters = useCallback(() => {
    updateFilters({
      collegeId: filterCollegeId ? (filterCollegeId as number) : undefined,
      name: searchName.trim() || undefined,
    });
  }, [filterCollegeId, searchName, updateFilters]);

  const handleClearFilters = useCallback(() => {
    setFilterCollegeId("");
    setSearchName("");
    updateFilters({ collegeId: undefined, name: undefined });
  }, [updateFilters]);

  const handleDelete = useCallback(
    (m: MajorRespDTO) => {
      if (window.confirm(`确定删除专业「${m.name}」吗？`)) {
        deleteMajor.mutate(m.id, {
          onSuccess: () => window.alert("删除成功"),
          onError: () => window.alert("删除失败，请重试"),
        });
      }
    },
    [deleteMajor],
  );

  const handleSubmit = useCallback(
    (data: MajorCreateDTO) => {
      if (editing) {
        updateMajor.mutate(
          { id: editing.id, data },
          {
            onSuccess: () => {
              setFormOpen(false);
              setEditing(null);
              window.alert("更新成功");
            },
            onError: () => window.alert("更新失败，请重试"),
          },
        );
      } else {
        createMajor.mutate(data, {
          onSuccess: () => {
            setFormOpen(false);
            window.alert("创建成功");
          },
          onError: () => window.alert("创建失败，请重试"),
        });
      }
    },
    [editing, createMajor, updateMajor],
  );

  const isSubmitting = createMajor.isPending || updateMajor.isPending;

  return (
    <div className="flex flex-col gap-4">
      {/* Toolbar */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <select
            value={filterCollegeId}
            onChange={(e) =>
              setFilterCollegeId(
                e.target.value ? Number(e.target.value) : "",
              )
            }
            className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="">全部院校</option>
            {colleges.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
          <Input
            value={searchName}
            onChange={(e) => setSearchName(e.target.value)}
            placeholder="搜索专业名称..."
            className="w-48"
            onKeyDown={(e) => {
              if (e.key === "Enter") handleApplyFilters();
            }}
          />
          <Button size="sm" onClick={handleApplyFilters}>
            <Search className="mr-1 h-3.5 w-3.5" />
            筛选
          </Button>
          <Button variant="ghost" size="sm" onClick={handleClearFilters}>
            <X className="mr-1 h-3.5 w-3.5" />
            清除
          </Button>
        </div>
        <Button
          onClick={() => {
            setEditing(null);
            setFormOpen(true);
          }}
        >
          <Plus className="mr-1.5 h-4 w-4" />
          新建专业
        </Button>
      </div>

      {/* Table */}
      <div className="rounded-lg border border-slate-200 bg-white">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-left">
                <th className="px-4 py-3 font-medium text-slate-600">ID</th>
                <th className="px-4 py-3 font-medium text-slate-600">院校</th>
                <th className="px-4 py-3 font-medium text-slate-600">名称</th>
                <th className="px-4 py-3 font-medium text-slate-600">代码</th>
                <th className="px-4 py-3 font-medium text-slate-600">类别</th>
                <th className="px-4 py-3 font-medium text-slate-600">招生对象</th>
                <th className="px-4 py-3 font-medium text-slate-600">年份</th>
                <th className="px-4 py-3 font-medium text-slate-600">操作</th>
              </tr>
            </thead>
            <tbody>
              {isLoading || isFetching ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-4 py-12 text-center text-slate-400"
                  >
                    加载中...
                  </td>
                </tr>
              ) : majors.length === 0 ? (
                <tr>
                  <td
                    colSpan={8}
                    className="px-4 py-12 text-center text-slate-400"
                  >
                    暂无专业数据
                  </td>
                </tr>
              ) : (
                majors.map((m) => (
                  <tr
                    key={m.id}
                    className="border-b border-slate-100 last:border-0 hover:bg-slate-50/50"
                  >
                    <td className="px-4 py-3 text-slate-500">{m.id}</td>
                    <td className="px-4 py-3 text-slate-600">
                      {collegeNameMap.get(m.collegeId ?? 0) ?? "-"}
                    </td>
                    <td className="max-w-[200px] truncate px-4 py-3 font-medium text-slate-800">
                      {m.name}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {m.code ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {m.category ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {m.targetType ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {m.year ?? "-"}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8"
                          onClick={() => {
                            setEditing(m);
                            setFormOpen(true);
                          }}
                          title="编辑"
                        >
                          <Pencil className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-red-500 hover:text-red-700"
                          onClick={() => handleDelete(m)}
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
        <Pagination
          total={total}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      </div>

      <MajorFormDialog
        open={formOpen}
        onOpenChange={(open) => {
          setFormOpen(open);
          if (!open) setEditing(null);
        }}
        onSubmit={handleSubmit}
        isSubmitting={isSubmitting}
        editing={editing}
        colleges={colleges}
      />
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// EXAM OUTLINES TAB
// ════════════════════════════════════════════════════════════════════

function ExamOutlineFormDialog({
  open,
  onOpenChange,
  onSubmit,
  isSubmitting,
  editing,
  colleges,
  majors,
  onCollegeChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: ExamOutlineCreateDTO) => void;
  isSubmitting: boolean;
  editing: ExamOutlineRespDTO | null;
  colleges: CollegeRespDTO[];
  majors: MajorRespDTO[];
  onCollegeChange: (collegeId: number | "") => void;
}) {
  const [collegeId, setCollegeId] = useState<number | "">("");
  const [majorId, setMajorId] = useState<number | "">("");
  const [title, setTitle] = useState("");
  const [year, setYear] = useState<number | "">("");
  const [content, setContent] = useState("");
  const [fileUrl, setFileUrl] = useState("");

  useEffect(() => {
    if (!open) return;
    if (editing) {
      /* eslint-disable react-hooks/set-state-in-effect -- form init from props */
      setCollegeId(editing.collegeId ?? "");
      setMajorId(editing.majorId ?? "");
      setTitle(editing.title ?? "");
      setYear(editing.year ?? "");
      setContent(editing.content ?? "");
      setFileUrl(editing.fileUrl ?? "");
      /* eslint-enable react-hooks/set-state-in-effect */
    } else {
      setCollegeId("");
      setMajorId("");
      setTitle("");
      setYear("");
      setContent("");
      setFileUrl("");
    }
  }, [open, editing]);

  function handleCollegeChange(value: string) {
    const numVal = value ? Number(value) : "";
    setCollegeId(numVal);
    setMajorId("");
    onCollegeChange(numVal);
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    onSubmit({
      collegeId: collegeId ? (collegeId as number) : undefined,
      majorId: majorId ? (majorId as number) : undefined,
      title: title.trim(),
      year: year ? (year as number) : undefined,
      content: content.trim() || undefined,
      fileUrl: fileUrl.trim() || undefined,
    });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            {editing ? "编辑考纲" : "新建考纲"}
          </DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit}>
          <ScrollArea className="max-h-[60vh]">
            <div className="space-y-4 pr-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label>所属院校</Label>
                  <select
                    value={collegeId}
                    onChange={(e) => handleCollegeChange(e.target.value)}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  >
                    <option value="">请选择院校</option>
                    {colleges.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="space-y-1.5">
                  <Label>所属专业</Label>
                  <select
                    value={majorId}
                    onChange={(e) =>
                      setMajorId(
                        e.target.value ? Number(e.target.value) : "",
                      )
                    }
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    disabled={!collegeId}
                  >
                    <option value="">请选择专业</option>
                    {majors.map((m) => (
                      <option key={m.id} value={m.id}>
                        {m.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label>
                    考纲标题 <span className="text-red-500">*</span>
                  </Label>
                  <Input
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="请输入考纲标题"
                    required
                  />
                </div>
                <div className="space-y-1.5">
                  <Label>年份</Label>
                  <Input
                    type="number"
                    value={year}
                    onChange={(e) =>
                      setYear(
                        e.target.value ? Number(e.target.value) : "",
                      )
                    }
                    placeholder="例如：2026"
                  />
                </div>
              </div>
              <div className="space-y-1.5">
                <Label>考纲内容</Label>
                <Textarea
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  placeholder="请输入考纲内容"
                  rows={6}
                />
              </div>
              <div className="space-y-1.5">
                <Label>附件链接</Label>
                <Input
                  value={fileUrl}
                  onChange={(e) => setFileUrl(e.target.value)}
                  placeholder="https://..."
                />
              </div>
            </div>
          </ScrollArea>
          <DialogFooter className="mt-6">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isSubmitting}
            >
              取消
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting || !title.trim()}
            >
              {isSubmitting
                ? "保存中..."
                : editing
                  ? "保存修改"
                  : "创建考纲"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function ExamOutlinesTab() {
  const {
    examOutlines,
    total,
    totalPages,
    currentPage,
    isLoading,
    isFetching,
    setPage,
    updateFilters,
    createExamOutline,
    updateExamOutline,
    deleteExamOutline,
  } = useExamOutlines();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<ExamOutlineRespDTO | null>(null);
  const [colleges, setColleges] = useState<CollegeRespDTO[]>([]);
  const [formMajors, setFormMajors] = useState<MajorRespDTO[]>([]);
  const [filterCollegeId, setFilterCollegeId] = useState<number | "">("");
  const [filterMajorId, setFilterMajorId] = useState<number | "">("");
  const [filterMajors, setFilterMajors] = useState<MajorRespDTO[]>([]);

  // Load colleges on mount
  useEffect(() => {
    collegeService.listColleges().then(setColleges).catch(() => {});
  }, []);

  // Load filter majors when filter college changes
  useEffect(() => {
    if (!filterCollegeId) {
      /* eslint-disable react-hooks/set-state-in-effect -- clearing dependent filter */
      setFilterMajors([]);
      setFilterMajorId("");
      /* eslint-enable react-hooks/set-state-in-effect */
      return;
    }
    let cancelled = false;
    collegeService
      .listMajors(filterCollegeId as number)
      .then((data) => {
        if (!cancelled) setFilterMajors(data);
      })
      .catch(() => {
        if (!cancelled) setFilterMajors([]);
      });
    return () => {
      cancelled = true;
    };
  }, [filterCollegeId]);

  const collegeNameMap = new Map(colleges.map((c) => [c.id, c.name]));

  // Build major name map from all loaded majors (filter + form)
  const allMajors = [...filterMajors, ...formMajors];
  const majorNameMap = new Map(
    allMajors.map((m) => [m.id, m.name]),
  );

  const handleFormCollegeChange = useCallback(
    (collegeId: number | "") => {
      if (!collegeId) {
        setFormMajors([]);
        return;
      }
      collegeService
        .listMajors(collegeId as number)
        .then(setFormMajors)
        .catch(() => setFormMajors([]));
    },
    [],
  );

  const handleApplyFilters = useCallback(() => {
    updateFilters({
      collegeId: filterCollegeId
        ? (filterCollegeId as number)
        : undefined,
      majorId: filterMajorId ? (filterMajorId as number) : undefined,
    });
  }, [filterCollegeId, filterMajorId, updateFilters]);

  const handleClearFilters = useCallback(() => {
    setFilterCollegeId("");
    setFilterMajorId("");
    setFilterMajors([]);
    updateFilters({ collegeId: undefined, majorId: undefined });
  }, [updateFilters]);

  const handleDelete = useCallback(
    (e: ExamOutlineRespDTO) => {
      if (window.confirm(`确定删除考纲「${e.title}」吗？`)) {
        deleteExamOutline.mutate(e.id, {
          onSuccess: () => window.alert("删除成功"),
          onError: () => window.alert("删除失败，请重试"),
        });
      }
    },
    [deleteExamOutline],
  );

  const handleSubmit = useCallback(
    (data: ExamOutlineCreateDTO) => {
      if (editing) {
        updateExamOutline.mutate(
          { id: editing.id, data },
          {
            onSuccess: () => {
              setFormOpen(false);
              setEditing(null);
              window.alert("更新成功");
            },
            onError: () => window.alert("更新失败，请重试"),
          },
        );
      } else {
        createExamOutline.mutate(data, {
          onSuccess: () => {
            setFormOpen(false);
            window.alert("创建成功");
          },
          onError: () => window.alert("创建失败，请重试"),
        });
      }
    },
    [editing, createExamOutline, updateExamOutline],
  );

  const isSubmitting =
    createExamOutline.isPending || updateExamOutline.isPending;

  return (
    <div className="flex flex-col gap-4">
      {/* Toolbar */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <select
            value={filterCollegeId}
            onChange={(e) => {
              const val = e.target.value ? Number(e.target.value) : "";
              setFilterCollegeId(val);
              setFilterMajorId("");
            }}
            className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="">全部院校</option>
            {colleges.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
          <select
            value={filterMajorId}
            onChange={(e) =>
              setFilterMajorId(
                e.target.value ? Number(e.target.value) : "",
              )
            }
            className="flex h-9 rounded-md border border-input bg-white px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            disabled={!filterCollegeId}
          >
            <option value="">全部专业</option>
            {filterMajors.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
              </option>
            ))}
          </select>
          <Button size="sm" onClick={handleApplyFilters}>
            <Search className="mr-1 h-3.5 w-3.5" />
            筛选
          </Button>
          <Button variant="ghost" size="sm" onClick={handleClearFilters}>
            <X className="mr-1 h-3.5 w-3.5" />
            清除
          </Button>
        </div>
        <Button
          onClick={() => {
            setEditing(null);
            setFormOpen(true);
          }}
        >
          <Plus className="mr-1.5 h-4 w-4" />
          新建考纲
        </Button>
      </div>

      {/* Table */}
      <div className="rounded-lg border border-slate-200 bg-white">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-left">
                <th className="px-4 py-3 font-medium text-slate-600">ID</th>
                <th className="px-4 py-3 font-medium text-slate-600">院校</th>
                <th className="px-4 py-3 font-medium text-slate-600">专业</th>
                <th className="px-4 py-3 font-medium text-slate-600">标题</th>
                <th className="px-4 py-3 font-medium text-slate-600">年份</th>
                <th className="px-4 py-3 font-medium text-slate-600">创建时间</th>
                <th className="px-4 py-3 font-medium text-slate-600">操作</th>
              </tr>
            </thead>
            <tbody>
              {isLoading || isFetching ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-4 py-12 text-center text-slate-400"
                  >
                    加载中...
                  </td>
                </tr>
              ) : examOutlines.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-4 py-12 text-center text-slate-400"
                  >
                    暂无考纲数据
                  </td>
                </tr>
              ) : (
                examOutlines.map((e) => (
                  <tr
                    key={e.id}
                    className="border-b border-slate-100 last:border-0 hover:bg-slate-50/50"
                  >
                    <td className="px-4 py-3 text-slate-500">{e.id}</td>
                    <td className="px-4 py-3 text-slate-600">
                      {collegeNameMap.get(e.collegeId ?? 0) ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {majorNameMap.get(e.majorId ?? 0) ?? "-"}
                    </td>
                    <td className="max-w-[200px] truncate px-4 py-3 font-medium text-slate-800">
                      {e.title ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-600">
                      {e.year ?? "-"}
                    </td>
                    <td className="px-4 py-3 text-slate-500">
                      {e.createTime ?? "-"}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8"
                          onClick={() => {
                            setEditing(e);
                            setFormOpen(true);
                            // Pre-load majors for the editing college
                            if (e.collegeId) {
                              collegeService
                                .listMajors(e.collegeId)
                                .then(setFormMajors)
                                .catch(() => setFormMajors([]));
                            }
                          }}
                          title="编辑"
                        >
                          <Pencil className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-red-500 hover:text-red-700"
                          onClick={() => handleDelete(e)}
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
        <Pagination
          total={total}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setPage}
        />
      </div>

      <ExamOutlineFormDialog
        open={formOpen}
        onOpenChange={(open) => {
          setFormOpen(open);
          if (!open) {
            setEditing(null);
            setFormMajors([]);
          }
        }}
        onSubmit={handleSubmit}
        isSubmitting={isSubmitting}
        editing={editing}
        colleges={colleges}
        majors={formMajors}
        onCollegeChange={handleFormCollegeChange}
      />
    </div>
  );
}

// ════════════════════════════════════════════════════════════════════
// MAIN PAGE
// ════════════════════════════════════════════════════════════════════

export default function TeacherCollegesPage() {
  const [activeTab, setActiveTab] = useState<TabKey>("colleges");

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="border-b border-slate-200 px-6 py-4">
        <h1 className="text-xl font-semibold text-slate-800">
          院校 / 专业 / 考纲管理
        </h1>
        <p className="mt-0.5 text-sm text-slate-500">
          管理院校信息、专业设置和考试大纲
        </p>
      </div>

      {/* Tabs */}
      <div className="border-b border-slate-200 bg-slate-50/50 px-6">
        <div className="flex gap-1">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={cn(
                "relative px-4 py-2.5 text-sm font-medium transition-colors",
                activeTab === tab.key
                  ? "text-slate-900 after:absolute after:inset-x-0 after:bottom-0 after:h-0.5 after:bg-slate-900"
                  : "text-slate-500 hover:text-slate-700",
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        {activeTab === "colleges" && <CollegesTab />}
        {activeTab === "majors" && <MajorsTab />}
        {activeTab === "examOutlines" && <ExamOutlinesTab />}
      </div>
    </div>
  );
}

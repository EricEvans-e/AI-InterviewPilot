import { useState } from "react";
import type { UseMutateFunction } from "@tanstack/react-query";
import { Loader2, ChevronDown, X } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useStudentProfile } from "@/hooks/profile/useStudentProfile";
import { authService } from "@/services/authService";
import type {
  StudentProfileRespDTO,
  StudentProfileSaveReqDTO,
} from "@/services/studentService";

const GRADE_OPTIONS = [
  "高一",
  "高二",
  "高三",
  "大一",
  "大二",
  "大三",
  "大四",
  "已毕业",
] as const;

const EXAM_CATEGORY_OPTIONS = ["普高招生", "单独考试招生"] as const;

const TRAINING_STAGE_OPTIONS = ["入门", "强化", "冲刺"] as const;

interface MultiSelectProps {
  label: string;
  options: { id: number; name: string }[];
  selected: number[];
  onChange: (ids: number[]) => void;
  placeholder?: string;
}

function MultiSelect({
  label,
  options,
  selected,
  onChange,
  placeholder = "请选择",
}: MultiSelectProps) {
  const [open, setOpen] = useState(false);

  const toggle = (id: number) => {
    onChange(
      selected.includes(id)
        ? selected.filter((item) => item !== id)
        : [...selected, id],
    );
  };

  const remove = (id: number) => {
    onChange(selected.filter((item) => item !== id));
  };

  const selectedItems = options.filter((opt) => selected.includes(opt.id));

  return (
    <div className="space-y-2">
      <Label>{label}</Label>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <button
            type="button"
            className="flex min-h-[40px] w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background hover:bg-accent/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          >
            <div className="flex flex-1 flex-wrap gap-1.5">
              {selectedItems.length === 0 ? (
                <span className="text-muted-foreground">{placeholder}</span>
              ) : (
                selectedItems.map((item) => (
                  <span
                    key={item.id}
                    className="inline-flex items-center gap-1 rounded-md bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary"
                  >
                    {item.name}
                    <span
                      role="button"
                      tabIndex={0}
                      className="cursor-pointer rounded-sm hover:bg-primary/20"
                      onClick={(e) => {
                        e.stopPropagation();
                        remove(item.id);
                      }}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.stopPropagation();
                          remove(item.id);
                        }
                      }}
                    >
                      <X className="h-3 w-3" />
                    </span>
                  </span>
                ))
              )}
            </div>
            <ChevronDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
          </button>
        </PopoverTrigger>
        <PopoverContent className="w-[var(--radix-popover-trigger-width)] p-0">
          <div className="max-h-60 overflow-y-auto p-2">
            {options.length === 0 ? (
              <p className="px-2 py-1.5 text-sm text-muted-foreground">
                暂无选项
              </p>
            ) : (
              options.map((option) => {
                const isChecked = selected.includes(option.id);
                return (
                  <label
                    key={option.id}
                    className="flex cursor-pointer items-center gap-2 rounded-sm px-2 py-1.5 text-sm hover:bg-accent"
                  >
                    <input
                      type="checkbox"
                      checked={isChecked}
                      onChange={() => toggle(option.id)}
                      className="h-4 w-4 rounded border-input accent-primary"
                    />
                    {option.name}
                  </label>
                );
              })
            )}
          </div>
        </PopoverContent>
      </Popover>
    </div>
  );
}

type StudentProfileFormProps = {
  profile: StudentProfileRespDTO;
  saveProfile: UseMutateFunction<void, Error, StudentProfileSaveReqDTO>;
  isSaving: boolean;
  saveError: Error | null;
};

function StudentProfileForm({
  profile,
  saveProfile,
  isSaving,
  saveError,
}: StudentProfileFormProps) {
  const [schoolName, setSchoolName] = useState(profile.schoolName ?? "");
  const [grade, setGrade] = useState(profile.grade ?? "");
  const [examCategory, setExamCategory] = useState(
    profile.examCategory ?? "",
  );
  const [trainingStage, setTrainingStage] = useState(
    profile.trainingStage ?? "",
  );
  const [collegeIds, setCollegeIds] = useState<number[]>(
    profile.targetColleges?.map((c) => c.id) ?? [],
  );
  const [majorIds, setMajorIds] = useState<number[]>(
    profile.targetMajors?.map((m) => m.id) ?? [],
  );
  const [saveSuccess, setSaveSuccess] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSaveSuccess(false);

    const payload: StudentProfileSaveReqDTO = {
      schoolName: schoolName.trim() || undefined,
      grade: grade || undefined,
      examCategory: examCategory || undefined,
      trainingStage: trainingStage || undefined,
      collegeIds: collegeIds.length > 0 ? collegeIds : undefined,
      majorIds: majorIds.length > 0 ? majorIds : undefined,
    };

    saveProfile(payload, {
      onSuccess: () => setSaveSuccess(true),
    });
  };

  const targetColleges = profile?.targetColleges ?? [];
  const targetMajors = profile?.targetMajors ?? [];

  return (
    <Card>
      <CardHeader>
        <CardTitle>个人信息</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* School Name */}
          <div className="space-y-2">
            <Label htmlFor="schoolName">学校名称</Label>
            <Input
              id="schoolName"
              value={schoolName}
              onChange={(e) => setSchoolName(e.target.value)}
              placeholder="请输入学校名称"
            />
          </div>

          {/* Grade */}
          <div className="space-y-2">
            <Label htmlFor="grade">年级</Label>
            <select
              id="grade"
              value={grade}
              onChange={(e) => setGrade(e.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <option value="">请选择年级</option>
              {GRADE_OPTIONS.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </div>

          {/* Exam Category */}
          <div className="space-y-2">
            <Label htmlFor="examCategory">考试类别</Label>
            <select
              id="examCategory"
              value={examCategory}
              onChange={(e) => setExamCategory(e.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <option value="">请选择考试类别</option>
              {EXAM_CATEGORY_OPTIONS.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </div>

          {/* Training Stage */}
          <div className="space-y-2">
            <Label htmlFor="trainingStage">训练阶段</Label>
            <select
              id="trainingStage"
              value={trainingStage}
              onChange={(e) => setTrainingStage(e.target.value)}
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <option value="">请选择训练阶段</option>
              {TRAINING_STAGE_OPTIONS.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </div>

          {/* Target Colleges */}
          <MultiSelect
            label="目标院校"
            options={targetColleges}
            selected={collegeIds}
            onChange={setCollegeIds}
            placeholder="请选择目标院校"
          />

          {/* Target Majors */}
          <MultiSelect
            label="目标专业"
            options={targetMajors}
            selected={majorIds}
            onChange={setMajorIds}
            placeholder="请选择目标专业"
          />

          {/* Status Messages */}
          {saveError && (
            <p className="text-sm text-red-600">
              保存失败：
              {saveError instanceof Error ? saveError.message : "请稍后重试"}
            </p>
          )}
          {saveSuccess && <p className="text-sm text-green-600">保存成功</p>}

          {/* Submit */}
          <div className="flex justify-end pt-2">
            <Button type="submit" disabled={isSaving}>
              {isSaving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              保存
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

function PasswordChangeCard() {
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccess(false);

    if (newPassword !== confirmPassword) {
      setError("两次输入的新密码不一致");
      return;
    }

    setIsSubmitting(true);
    try {
      await authService.changePassword({
        oldPassword,
        newPassword,
        confirmPassword,
      });
      setSuccess(true);
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "密码修改失败");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>修改密码</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="oldPassword">当前密码</Label>
            <Input
              id="oldPassword"
              type="password"
              value={oldPassword}
              onChange={(e) => setOldPassword(e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="newPassword">新密码</Label>
            <Input
              id="newPassword"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="confirmPassword">确认新密码</Label>
            <Input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}
          {success && <p className="text-sm text-green-600">密码修改成功</p>}

          <div className="flex justify-end">
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              修改密码
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

const getProfileFormKey = (profile: StudentProfileRespDTO) =>
  JSON.stringify({
    schoolName: profile.schoolName ?? "",
    grade: profile.grade ?? "",
    examCategory: profile.examCategory ?? "",
    trainingStage: profile.trainingStage ?? "",
    collegeIds: profile.targetColleges?.map((item) => item.id) ?? [],
    majorIds: profile.targetMajors?.map((item) => item.id) ?? [],
  });

export default function StudentProfilePage() {
  const { profile, isLoading, error, saveProfile, isSaving, saveError } =
    useStudentProfile();

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-full items-center justify-center">
        <Card className="max-w-md border-red-200 bg-red-50 p-6 text-center">
          <p className="text-sm text-red-600">
            加载个人信息失败，请稍后重试。
          </p>
          <p className="mt-1 text-xs text-red-400">
            {error instanceof Error ? error.message : "未知错误"}
          </p>
        </Card>
      </div>
    );
  }

  return (
    <div className="h-full overflow-y-auto bg-white">
      <div className="mx-auto max-w-2xl space-y-6 px-6 py-10">
        <StudentProfileForm
          key={profile ? getProfileFormKey(profile) : "empty-profile"}
          profile={profile ?? {}}
          saveProfile={saveProfile}
          isSaving={isSaving}
          saveError={saveError}
        />
        <PasswordChangeCard />
      </div>
    </div>
  );
}

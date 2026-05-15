import { useState, useCallback } from "react";
import { Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import type { ReviewSubmitDTO } from "@/services/teacherService";

interface TeacherReviewFormProps {
  sessionId: string;
  isSubmitting: boolean;
  onSubmit: (data: ReviewSubmitDTO) => void;
}

export default function TeacherReviewForm({
  sessionId,
  isSubmitting,
  onSubmit,
}: TeacherReviewFormProps) {
  const [comment, setComment] = useState("");
  const [score, setScore] = useState<string>("");
  const [markExcellent, setMarkExcellent] = useState(false);
  const [markMisjudge, setMarkMisjudge] = useState(false);

  const handleSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();

      if (!comment.trim()) {
        window.alert("请输入点评内容");
        return;
      }

      const data: ReviewSubmitDTO = {
        sessionId,
        content: comment.trim(),
        isExcellentSample: markExcellent || undefined,
        isModelMisjudge: markMisjudge || undefined,
      };

      if (score.trim() !== "") {
        const numScore = Number(score);
        if (Number.isNaN(numScore) || numScore < 0 || numScore > 100) {
          window.alert("调整分数须为 0-100 之间的数字");
          return;
        }
        data.adjustedScore = numScore;
      }

      onSubmit(data);
    },
    [sessionId, comment, score, markExcellent, markMisjudge, onSubmit],
  );

  const handleReset = useCallback(() => {
    setComment("");
    setScore("");
    setMarkExcellent(false);
    setMarkMisjudge(false);
  }, []);

  return (
    <Card className="border-slate-100">
      <CardHeader className="pb-3">
        <CardTitle className="text-base font-semibold text-slate-800">
          教师点评
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Comment */}
          <div className="space-y-1.5">
            <Label htmlFor="review-comment" className="text-sm text-slate-700">
              点评内容 <span className="text-red-500">*</span>
            </Label>
            <Textarea
              id="review-comment"
              placeholder="请输入对该学生面试表现的点评意见..."
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              className="min-h-[120px] resize-none"
              disabled={isSubmitting}
            />
          </div>

          {/* Score adjustment */}
          <div className="space-y-1.5">
            <Label htmlFor="review-score" className="text-sm text-slate-700">
              调整分数
              <span className="ml-1 text-xs text-slate-400">(可选，0-100)</span>
            </Label>
            <Input
              id="review-score"
              type="number"
              min={0}
              max={100}
              placeholder="留空则不调整 AI 评分"
              value={score}
              onChange={(e) => setScore(e.target.value)}
              className="w-48"
              disabled={isSubmitting}
            />
          </div>

          <Separator />

          {/* Checkboxes */}
          <div className="space-y-3">
            <label
              className={cn(
                "flex items-center gap-3 rounded-lg border border-slate-200 p-3 cursor-pointer transition-colors",
                markExcellent
                  ? "border-emerald-300 bg-emerald-50"
                  : "hover:bg-slate-50",
              )}
            >
              <input
                type="checkbox"
                checked={markExcellent}
                onChange={(e) => setMarkExcellent(e.target.checked)}
                className="h-4 w-4 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500"
                disabled={isSubmitting}
              />
              <div>
                <p className="text-sm font-medium text-slate-800">
                  标记优秀样本
                </p>
                <p className="text-xs text-slate-500">
                  标记为优秀面试案例，可用于教学参考
                </p>
              </div>
            </label>

            <label
              className={cn(
                "flex items-center gap-3 rounded-lg border border-slate-200 p-3 cursor-pointer transition-colors",
                markMisjudge
                  ? "border-amber-300 bg-amber-50"
                  : "hover:bg-slate-50",
              )}
            >
              <input
                type="checkbox"
                checked={markMisjudge}
                onChange={(e) => setMarkMisjudge(e.target.checked)}
                className="h-4 w-4 rounded border-slate-300 text-amber-600 focus:ring-amber-500"
                disabled={isSubmitting}
              />
              <div>
                <p className="text-sm font-medium text-slate-800">
                  标记模型误判
                </p>
                <p className="text-xs text-slate-500">
                  AI 评分或反馈存在明显偏差，需人工修正
                </p>
              </div>
            </label>
          </div>

          {/* Actions */}
          <div className="flex items-center gap-2 pt-2">
            <Button type="submit" disabled={isSubmitting} className="gap-1.5">
              <Send className="h-4 w-4" />
              {isSubmitting ? "提交中..." : "提交点评"}
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={handleReset}
              disabled={isSubmitting}
            >
              重置
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

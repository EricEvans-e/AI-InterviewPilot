import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2, Bot, Cpu } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { adminService, type SceneBinding } from "@/services/adminService";
import { cn } from "@/lib/utils";

const PROVIDER_LABELS: Record<string, string> = {
  xingchen: "Legacy Xunfei",
  openai: "Mimo OpenAI",
  anthropic: "Legacy Anthropic",
};

const PROVIDER_COLORS: Record<string, string> = {
  xingchen: "bg-blue-100 text-blue-700 border-blue-200",
  openai: "bg-emerald-100 text-emerald-700 border-emerald-200",
  anthropic: "bg-indigo-100 text-indigo-700 border-indigo-200",
};

export default function AdminAgentConfigPage() {
  const queryClient = useQueryClient();

  const {
    data: sceneBindings,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["admin-scene-bindings"],
    queryFn: () => adminService.getSceneBindings(),
  });

  const switchAgentMutation = useMutation({
    mutationFn: ({ sceneCode, agentId }: { sceneCode: string; agentId: number }) =>
      adminService.switchSceneAgent(sceneCode, agentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-scene-bindings"] });
    },
  });

  const handleAgentChange = (sceneCode: string, agentId: string) => {
    switchAgentMutation.mutate({ sceneCode, agentId: Number(agentId) });
  };

  const getProviderBadgeClass = (provider: string) => {
    return PROVIDER_COLORS[provider] ?? "bg-slate-100 text-slate-700 border-slate-200";
  };

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="border-b border-slate-200 px-6 py-4">
        <h1 className="text-xl font-semibold text-slate-800">面试链路配置</h1>
        <p className="mt-0.5 text-sm text-slate-500">
          管理各面试环节使用的 AI 模型和链路
        </p>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        {isLoading ? (
          <div className="flex h-48 items-center justify-center">
            <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
          </div>
        ) : error ? (
          <div className="flex h-48 items-center justify-center text-sm text-red-500">
            加载失败，请刷新重试
          </div>
        ) : !sceneBindings || sceneBindings.length === 0 ? (
          <div className="flex h-48 items-center justify-center text-sm text-slate-400">
            暂无场景配置
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
            {sceneBindings.map((binding) => (
              <SceneCard
                key={binding.sceneCode}
                binding={binding}
                isSwitching={switchAgentMutation.isPending}
                onAgentChange={handleAgentChange}
                getProviderBadgeClass={getProviderBadgeClass}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function SceneCard({
  binding,
  isSwitching,
  onAgentChange,
  getProviderBadgeClass,
}: {
  binding: SceneBinding;
  isSwitching: boolean;
  onAgentChange: (sceneCode: string, agentId: string) => void;
  getProviderBadgeClass: (provider: string) => string;
}) {
  const activeValue =
    binding.activeAgentId != null ? String(binding.activeAgentId) : undefined;

  return (
    <Card className="border-slate-100">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2 text-base font-semibold text-slate-800">
            <Bot className="h-5 w-5 text-slate-500" />
            {binding.sceneName}
          </CardTitle>
          {binding.activeProvider && (
            <Badge
              variant="outline"
              className={cn("text-xs", getProviderBadgeClass(binding.activeProvider))}
            >
              {PROVIDER_LABELS[binding.activeProvider] ?? binding.activeProvider}
            </Badge>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        {/* Current active agent */}
        <div className="flex items-center gap-2 rounded-md bg-slate-50 px-3 py-2">
          <Cpu className="h-4 w-4 text-slate-400" />
          <span className="text-sm text-slate-500">当前:</span>
          <span className="text-sm font-medium text-slate-800">
            {binding.activeAgentName || "未配置"}
          </span>
        </div>

        {/* Agent selector */}
        <div>
          <label className="mb-1.5 block text-xs font-medium text-slate-500">
            切换 Agent
          </label>
          <Select
            value={activeValue}
            onValueChange={(value) => onAgentChange(binding.sceneCode, value)}
            disabled={isSwitching}
          >
            <SelectTrigger className="h-9 text-sm">
              <SelectValue placeholder="选择 Agent" />
            </SelectTrigger>
            <SelectContent>
              {binding.candidates.map((candidate) => (
                <SelectItem key={candidate.id} value={String(candidate.id)}>
                  <div className="flex items-center gap-2">
                    <span>{candidate.agentName}</span>
                    <span className="text-xs text-slate-400">
                      ({PROVIDER_LABELS[candidate.aiProvider] ?? candidate.aiProvider})
                    </span>
                  </div>
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </CardContent>
    </Card>
  );
}

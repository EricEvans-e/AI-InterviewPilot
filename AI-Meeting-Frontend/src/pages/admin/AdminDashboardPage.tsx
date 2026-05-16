import { useQuery } from "@tanstack/react-query";
import { Users, Activity, TrendingUp, BarChart3 } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { adminService } from "@/services/adminService";

export default function AdminDashboardPage() {
  const { data: statsData } = useQuery({
    queryKey: ["admin-stats"],
    queryFn: () => adminService.getStats(),
  });

  const stats = [
    {
      label: "注册用户数",
      value: statsData?.totalUsers ?? "--",
      icon: Users,
      color: "text-blue-600",
      bgColor: "bg-blue-50",
    },
    {
      label: "今日活跃",
      value: statsData?.todayActive ?? "--",
      icon: Activity,
      color: "text-green-600",
      bgColor: "bg-green-50",
    },
    {
      label: "本周训练次数",
      value: statsData?.weekTrainingCount ?? "--",
      icon: TrendingUp,
      color: "text-orange-600",
      bgColor: "bg-orange-50",
    },
    {
      label: "平均得分",
      value: statsData?.avgScore != null ? statsData.avgScore.toFixed(1) : "--",
      icon: BarChart3,
      color: "text-purple-600",
      bgColor: "bg-purple-50",
    },
  ];

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="border-b border-slate-200 px-6 py-4">
        <h1 className="text-xl font-semibold text-slate-800">数据总览</h1>
        <p className="mt-0.5 text-sm text-slate-500">
          系统运行概况与核心数据指标
        </p>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {stats.map((stat) => (
            <Card key={stat.label} className="border-slate-100">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <CardTitle className="text-sm font-medium text-slate-600">
                  {stat.label}
                </CardTitle>
                <div className={`rounded-md p-1.5 ${stat.bgColor}`}>
                  <stat.icon className={`h-4 w-4 ${stat.color}`} />
                </div>
              </CardHeader>
              <CardContent>
                <p className="text-2xl font-bold text-slate-900">
                  {stat.value}
                </p>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Placeholder section */}
        <Card className="mt-6 border-slate-100">
          <CardHeader>
            <CardTitle className="text-base font-semibold text-slate-800">
              数据趋势
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex h-48 items-center justify-center rounded-lg bg-slate-50 text-sm text-slate-400">
              图表功能开发中，敬请期待
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

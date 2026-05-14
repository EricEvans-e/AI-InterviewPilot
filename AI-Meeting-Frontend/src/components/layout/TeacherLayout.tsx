import { NavLink, Outlet } from "react-router-dom";
import { LayoutDashboard, BookOpen, Users, Building2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/lib/constants";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";

const NAV_ITEMS = [
  {
    label: "总览",
    path: ROUTES.teacherDashboard,
    icon: LayoutDashboard,
  },
  {
    label: "题库管理",
    path: ROUTES.teacherQuestions,
    icon: BookOpen,
  },
  {
    label: "学生报告",
    path: ROUTES.teacherStudents,
    icon: Users,
  },
  {
    label: "院校管理",
    path: ROUTES.teacherColleges,
    icon: Building2,
  },
] as const;

export default function TeacherLayout() {
  return (
    <div className="flex h-full">
      {/* Sidebar */}
      <aside className="hidden w-56 flex-shrink-0 border-r border-slate-200 bg-slate-50 md:block">
        <div className="flex h-14 items-center px-4">
          <h2 className="text-base font-semibold text-slate-800">教师工作台</h2>
        </div>
        <Separator />
        <ScrollArea className="h-[calc(100%-3.5rem)]">
          <nav className="flex flex-col gap-1 p-3">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === ROUTES.teacherDashboard}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                    isActive
                      ? "bg-slate-200 text-slate-900"
                      : "text-slate-600 hover:bg-slate-100 hover:text-slate-900",
                  )
                }
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))}
          </nav>
        </ScrollArea>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}

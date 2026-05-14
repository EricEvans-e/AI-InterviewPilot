import { Link, useLocation } from "react-router-dom";
import { MessageSquare, Video, Layout, User, GraduationCap, Shield } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ROUTES } from "@/lib/constants";
import { cn } from "@/lib/utils";
import { useAppSelector } from "@/store/hooks";
import { selectUserRole } from "@/store/slices/userSlice";

type SidebarNavProps = {
  isCollapsed?: boolean;
};

export default function SidebarNav({ isCollapsed }: SidebarNavProps) {
  const location = useLocation();
  const role = useAppSelector(selectUserRole);
  const isActive = (path: string) =>
    path === ROUTES.chat
      ? location.pathname === ROUTES.chat ||
        location.pathname.startsWith(`${ROUTES.chat}/`)
      : location.pathname === path;

  return (
    <div className="px-3">
      <div className="space-y-1">
        <Link to={ROUTES.chat}>
          <Button
            variant={isActive(ROUTES.chat) ? "secondary" : "ghost"}
            className={cn(
              "w-full rounded-full",
              isCollapsed ? "justify-center" : "justify-start",
            )}
          >
            <MessageSquare className={cn("h-4 w-4", !isCollapsed && "mr-2")} />
            {!isCollapsed && "新对话"}
          </Button>
        </Link>

        <Link to={ROUTES.interviewIntro}>
          <Button
            variant={isActive(ROUTES.interviewIntro) ? "secondary" : "ghost"}
            className={cn(
              "w-full rounded-full",
              isCollapsed ? "justify-center" : "justify-start",
            )}
          >
            <Video className={cn("h-4 w-4", !isCollapsed && "mr-2")} />
            {!isCollapsed && "AI 面试"}
          </Button>
        </Link>

        <Link to={ROUTES.lobby}>
          <Button
            variant={isActive(ROUTES.lobby) ? "secondary" : "ghost"}
            className={cn(
              "w-full rounded-full",
              isCollapsed ? "justify-center" : "justify-start",
            )}
          >
            <Layout className={cn("h-4 w-4", !isCollapsed && "mr-2")} />
            {!isCollapsed && "面试大厅"}
          </Button>
        </Link>

        <Link to={ROUTES.studentProfile}>
          <Button
            variant={isActive(ROUTES.studentProfile) ? "secondary" : "ghost"}
            className={cn(
              "w-full rounded-full",
              isCollapsed ? "justify-center" : "justify-start",
            )}
          >
            <User className={cn("h-4 w-4", !isCollapsed && "mr-2")} />
            {!isCollapsed && "个人中心"}
          </Button>
        </Link>

        {(role === "teacher" || role === "admin") && (
          <Link to={ROUTES.teacherDashboard}>
            <Button
              variant={isActive(ROUTES.teacherDashboard) ? "secondary" : "ghost"}
              className={cn(
                "w-full rounded-full",
                isCollapsed ? "justify-center" : "justify-start",
              )}
            >
              <GraduationCap className={cn("h-4 w-4", !isCollapsed && "mr-2")} />
              {!isCollapsed && "教师后台"}
            </Button>
          </Link>
        )}

        {role === "admin" && (
          <Link to={ROUTES.adminDashboard}>
            <Button
              variant={isActive(ROUTES.adminDashboard) ? "secondary" : "ghost"}
              className={cn(
                "w-full rounded-full",
                isCollapsed ? "justify-center" : "justify-start",
              )}
            >
              <Shield className={cn("h-4 w-4", !isCollapsed && "mr-2")} />
              {!isCollapsed && "管理后台"}
            </Button>
          </Link>
        )}
      </div>
    </div>
  );
}

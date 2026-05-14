import { Suspense, lazy, type ReactNode } from "react";
import {
  Navigate,
  createBrowserRouter,
  type RouteObject,
} from "react-router-dom";
import { Loader2 } from "lucide-react";
import AuthGuard from "@/components/auth/AuthGuard";
import RoleGuard from "@/components/auth/RoleGuard";
import AppLayout from "@/layouts/AppLayout";
import TeacherLayout from "@/components/layout/TeacherLayout";
import AdminLayout from "@/components/layout/AdminLayout";
import { ROUTES } from "@/lib/constants";

// Public pages
const AuthPage = lazy(() => import("@/pages/auth/AuthPage"));
const MarketingHomePage = lazy(
  () => import("@/pages/marketing/MarketingHomePage"),
);

// Student pages
const ChatPage = lazy(() => import("@/pages/chat/ChatPage"));
const LobbyPage = lazy(() => import("@/pages/lobby/LobbyPage"));
const InterviewIntroPage = lazy(
  () => import("@/pages/interview/InterviewIntroPage"),
);
const InterviewPage = lazy(() => import("@/pages/interview/InterviewPage"));
const InterviewPrecheckPage = lazy(
  () => import("@/pages/interview/InterviewPrecheckPage"),
);
const InterviewReportPage = lazy(
  () => import("@/pages/interview/InterviewReportPage"),
);
const InterviewReportDetailPage = lazy(
  () => import("@/pages/interview/InterviewReportDetailPage"),
);
const StudentProfilePage = lazy(
  () => import("@/pages/profile/StudentProfilePage"),
);

// Teacher pages
const TeacherDashboardPage = lazy(
  () => import("@/pages/teacher/TeacherDashboardPage"),
);
const TeacherQuestionsPage = lazy(
  () => import("@/pages/teacher/TeacherQuestionsPage"),
);
const TeacherStudentsPage = lazy(
  () => import("@/pages/teacher/TeacherStudentsPage"),
);
const TeacherCollegesPage = lazy(
  () => import("@/pages/teacher/TeacherCollegesPage"),
);

// Admin pages
const AdminDashboardPage = lazy(
  () => import("@/pages/admin/AdminDashboardPage"),
);
const AdminUsersPage = lazy(() => import("@/pages/admin/AdminUsersPage"));

function RouteLoadingScreen() {
  return (
    <div className="flex min-h-[40vh] items-center justify-center bg-white">
      <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
    </div>
  );
}

const withRouteSuspense = (node: ReactNode) => (
  <Suspense fallback={<RouteLoadingScreen />}>{node}</Suspense>
);

export const appRoutes: RouteObject[] = [
  {
    path: ROUTES.home,
    element: <AppLayout />,
    children: [
      // Public pages
      {
        index: true,
        element: withRouteSuspense(<MarketingHomePage />),
      },
      {
        path: ROUTES.auth,
        element: withRouteSuspense(<AuthPage />),
      },

      // Student pages (any authenticated user)
      {
        element: <AuthGuard />,
        children: [
          {
            path: ROUTES.interviewIntro,
            element: withRouteSuspense(<InterviewIntroPage />),
          },
          {
            path: ROUTES.lobby,
            element: withRouteSuspense(<LobbyPage />),
          },
          {
            path: ROUTES.interviewPrecheck,
            element: withRouteSuspense(<InterviewPrecheckPage />),
          },
          {
            path: ROUTES.interviewRoom,
            element: withRouteSuspense(<InterviewPage />),
          },
          {
            path: `${ROUTES.interviewRoom}/:sessionId`,
            element: withRouteSuspense(<InterviewPage />),
          },
          {
            path: ROUTES.interviewReport,
            element: withRouteSuspense(<InterviewReportPage />),
          },
          {
            path: ROUTES.interviewReportDetail,
            element: withRouteSuspense(<InterviewReportDetailPage />),
          },
          {
            path: `${ROUTES.chat}/:sessionId?`,
            element: withRouteSuspense(<ChatPage />),
          },
          {
            path: ROUTES.questionBank,
            element: <Navigate to={ROUTES.chat} replace />,
          },
          {
            path: ROUTES.questionBankManage,
            element: <Navigate to={ROUTES.chat} replace />,
          },
          {
            path: ROUTES.studentProfile,
            element: withRouteSuspense(<StudentProfilePage />),
          },
        ],
      },

      // Teacher pages (teacher + admin roles)
      {
        element: <AuthGuard />,
        children: [
          {
            element: <RoleGuard allowedRoles={["teacher", "admin"]} />,
            children: [
              {
                element: <TeacherLayout />,
                children: [
                  {
                    path: ROUTES.teacherDashboard,
                    element: withRouteSuspense(<TeacherDashboardPage />),
                  },
                  {
                    path: ROUTES.teacherQuestions,
                    element: withRouteSuspense(<TeacherQuestionsPage />),
                  },
                  {
                    path: ROUTES.teacherStudents,
                    element: withRouteSuspense(<TeacherStudentsPage />),
                  },
                  {
                    path: ROUTES.teacherColleges,
                    element: withRouteSuspense(<TeacherCollegesPage />),
                  },
                ],
              },
            ],
          },
        ],
      },

      // Admin pages (admin only)
      {
        element: <AuthGuard />,
        children: [
          {
            element: <RoleGuard allowedRoles={["admin"]} />,
            children: [
              {
                element: <AdminLayout />,
                children: [
                  {
                    path: ROUTES.adminDashboard,
                    element: withRouteSuspense(<AdminDashboardPage />),
                  },
                  {
                    path: ROUTES.adminUsers,
                    element: withRouteSuspense(<AdminUsersPage />),
                  },
                ],
              },
            ],
          },
        ],
      },
    ],
  },
];

export const appRouter = createBrowserRouter(appRoutes);

import { Navigate, Outlet } from "react-router-dom";
import { useAppSelector } from "@/store/hooks";
import { selectUserRole } from "@/store/slices/userSlice";
import { ROUTES } from "@/lib/constants";
import type { UserRole } from "@/types/auth";

type RoleGuardProps = {
  allowedRoles: UserRole[];
};

export default function RoleGuard({ allowedRoles }: RoleGuardProps) {
  const role = useAppSelector(selectUserRole);

  if (!allowedRoles.includes(role)) {
    return <Navigate to={ROUTES.home} replace />;
  }

  return <Outlet />;
}

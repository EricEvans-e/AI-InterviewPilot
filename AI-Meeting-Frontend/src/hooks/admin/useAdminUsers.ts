import { useState, useCallback } from "react";
import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";
import { adminService } from "@/services/adminService";
import type { UserPageParams } from "@/services/adminService";

const USERS_QUERY_KEY = "admin-users";
const PAGE_SIZE = 10;

export function useAdminUsers() {
  const queryClient = useQueryClient();

  const [filters, setFilters] = useState<UserPageParams>({
    current: 1,
    size: PAGE_SIZE,
  });

  // ── Query ──
  const { data, isLoading, isFetching, refetch } = useQuery({
    queryKey: [USERS_QUERY_KEY, filters],
    queryFn: () => adminService.pageUsers(filters),
  });

  const users = data?.records ?? [];
  const total = data?.total ?? 0;
  const totalPages = data?.pages ?? 0;

  // ── Mutations ──
  const addAdminMutation = useMutation({
    mutationFn: (username: string) => adminService.addAdmin(username),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [USERS_QUERY_KEY] });
    },
  });

  // ── Helpers ──
  const setPage = useCallback((page: number) => {
    setFilters((prev) => ({ ...prev, current: page }));
  }, []);

  const updateFilters = useCallback(
    (patch: Partial<UserPageParams>) => {
      setFilters((prev) => ({ ...prev, ...patch, current: 1 }));
    },
    [],
  );

  return {
    // Data
    users,
    total,
    totalPages,
    currentPage: filters.current ?? 1,
    pageSize: PAGE_SIZE,
    filters,

    // Loading
    isLoading,
    isFetching,

    // Actions
    setPage,
    updateFilters,
    refetch,

    // Mutations
    addAdmin: addAdminMutation,
  };
}

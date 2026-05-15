import service from "@/lib/request";

// ── User DTOs ──

export interface UserPageRespDTO {
  id: number;
  username: string;
  realName?: string;
  phone?: string;
  mail?: string;
  delFlag?: number;
  createTime?: string;
  updateTime?: string;
  role?: string;
  isAdmin?: boolean;
}

export interface UserPageParams {
  current?: number;
  size?: number;
  keyword?: string;
  status?: number;
  createTimeSort?: string;
}

export interface UserPageResult {
  records: UserPageRespDTO[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// ── Service ──

export const adminService = {
  pageUsers(params: UserPageParams): Promise<UserPageResult> {
    return service.get<UserPageResult>("/ip/v1/users/page", { params });
  },

  addAdmin(username: string): Promise<void> {
    return service.post<void>("/ip/v1/users/admin", { username });
  },
};

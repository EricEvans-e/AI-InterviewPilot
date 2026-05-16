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

// ── Admin Stats ──

export interface AdminStatsResult {
  totalUsers: number;
  todayActive: number;
  weekTrainingCount: number;
  avgScore: number | null;
}

// ── Scene Bindings ──

export interface SceneBinding {
  sceneCode: string;
  sceneName: string;
  activeAgentId: number | null;
  activeAgentName: string;
  activeProvider: string;
  candidates: { id: number; agentName: string; aiProvider: string }[];
}

// ── Service ──

export const adminService = {
  pageUsers(params: UserPageParams): Promise<UserPageResult> {
    return service.get<UserPageResult>("/ip/v1/users/page", { params });
  },

  addAdmin(username: string): Promise<void> {
    return service.post<void>("/ip/v1/users/admin", { username });
  },

  getStats(): Promise<AdminStatsResult> {
    return service.get<AdminStatsResult>("/ip/v1/users/stats");
  },

  getSceneBindings(): Promise<SceneBinding[]> {
    return service.get<SceneBinding[]>("/ip/v1/agent-properties/scene-bindings");
  },

  switchSceneAgent(sceneCode: string, agentId: number): Promise<void> {
    return service.put<void>(`/ip/v1/agent-properties/scene-bindings/${sceneCode}/active/${agentId}`);
  },
};

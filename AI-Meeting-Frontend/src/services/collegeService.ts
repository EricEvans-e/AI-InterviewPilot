import service from "@/lib/request";
import type {
  CollegeRespDTO,
  MajorRespDTO,
  PageInfo,
} from "@/services/questionBankService";

// ── Exam Outline DTO ──

export interface ExamOutlineRespDTO {
  id: number;
  collegeId?: number;
  majorId?: number;
  title?: string;
  year?: number;
  content?: string;
  fileUrl?: string;
  createTime?: string;
  updateTime?: string;
}

// ── Create / Update DTOs ──

export interface CollegeCreateDTO {
  name: string;
  code?: string;
  type?: string;
  province?: string;
  city?: string;
  level?: string;
  officialUrl?: string;
  remark?: string;
}

export interface MajorCreateDTO {
  collegeId: number;
  name: string;
  code?: string;
  category?: string;
  targetType?: string;
  testForm?: string;
  testContent?: string;
  scoreStructure?: string;
  year?: number;
  officialUrl?: string;
}

export interface ExamOutlineCreateDTO {
  collegeId?: number;
  majorId?: number;
  title: string;
  year?: number;
  content?: string;
  fileUrl?: string;
}

// ── Page Params ──

export interface CollegePageParams {
  name?: string;
  province?: string;
  city?: string;
  type?: string;
  level?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface MajorPageParams {
  collegeId?: number;
  name?: string;
  category?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface ExamOutlinePageParams {
  collegeId?: number;
  majorId?: number;
  year?: number;
  pageNum?: number;
  pageSize?: number;
}

// ── Service ──

export const collegeService = {
  // ── Colleges ──
  pageColleges(
    params: CollegePageParams,
  ): Promise<PageInfo<CollegeRespDTO>> {
    return service.get<PageInfo<CollegeRespDTO>>(
      "/ip/v1/colleges/page",
      { params },
    );
  },

  listColleges(): Promise<CollegeRespDTO[]> {
    return service.get<CollegeRespDTO[]>("/ip/v1/colleges/list");
  },

  createCollege(data: CollegeCreateDTO): Promise<void> {
    return service.post<void>("/ip/v1/colleges", data);
  },

  updateCollege(id: number, data: Partial<CollegeCreateDTO>): Promise<void> {
    return service.put<void>(`/ip/v1/colleges/${id}`, data);
  },

  deleteCollege(id: number): Promise<void> {
    return service.delete<void>(`/ip/v1/colleges/${id}`);
  },

  // ── Majors ──
  pageMajors(params: MajorPageParams): Promise<PageInfo<MajorRespDTO>> {
    return service.get<PageInfo<MajorRespDTO>>("/ip/v1/majors/page", {
      params,
    });
  },

  listMajors(collegeId?: number): Promise<MajorRespDTO[]> {
    return service.get<MajorRespDTO[]>("/ip/v1/majors/list", {
      params: collegeId ? { collegeId } : undefined,
    });
  },

  createMajor(data: MajorCreateDTO): Promise<void> {
    return service.post<void>("/ip/v1/majors", data);
  },

  updateMajor(id: number, data: Partial<MajorCreateDTO>): Promise<void> {
    return service.put<void>(`/ip/v1/majors/${id}`, data);
  },

  deleteMajor(id: number): Promise<void> {
    return service.delete<void>(`/ip/v1/majors/${id}`);
  },

  // ── Exam Outlines ──
  pageExamOutlines(
    params: ExamOutlinePageParams,
  ): Promise<PageInfo<ExamOutlineRespDTO>> {
    return service.get<PageInfo<ExamOutlineRespDTO>>(
      "/ip/v1/exam-outlines/page",
      { params },
    );
  },

  listExamOutlines(): Promise<ExamOutlineRespDTO[]> {
    return service.get<ExamOutlineRespDTO[]>(
      "/ip/v1/exam-outlines/list",
    );
  },

  createExamOutline(data: ExamOutlineCreateDTO): Promise<void> {
    return service.post<void>("/ip/v1/exam-outlines", data);
  },

  updateExamOutline(
    id: number,
    data: Partial<ExamOutlineCreateDTO>,
  ): Promise<void> {
    return service.put<void>(`/ip/v1/exam-outlines/${id}`, data);
  },

  deleteExamOutline(id: number): Promise<void> {
    return service.delete<void>(`/ip/v1/exam-outlines/${id}`);
  },
};

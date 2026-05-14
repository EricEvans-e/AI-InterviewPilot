import service from "@/lib/request";
import type {
  QuestionRespDTO,
  QuestionPageParams,
  PageInfo,
  CollegeRespDTO,
  MajorRespDTO,
} from "@/services/questionBankService";

// ── Question DTOs ──

export interface QuestionCreateDTO {
  title: string;
  content?: string;
  questionType: string;
  collegeId?: number;
  majorId?: number;
  abilityTag?: string;
  difficulty?: string;
  answerTimeSeconds?: number;
  referenceAnswer?: string;
  scoringRule?: string;
  followUpRule?: string;
}

export type QuestionUpdateDTO = Partial<QuestionCreateDTO>;

export interface AiGenerateParams {
  collegeId?: number;
  majorId?: number;
  questionType?: string;
  abilityTag?: string;
  count: number;
  difficulty?: string;
  generateFollowUp?: boolean;
  generateScoringRule?: boolean;
}

export interface AiGenerateResult {
  questions: QuestionCreateDTO[];
}

// ── Student report DTOs ──

export interface StudentReportDTO {
  id: number;
  studentName?: string;
  studentId?: number;
  sessionTitle?: string;
  collegeName?: string;
  majorName?: string;
  overallScore?: number;
  status?: string;
  createTime?: string;
}

export interface StudentReportPageParams {
  collegeId?: number;
  majorId?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface ReviewSubmitDTO {
  reportId: number;
  score?: number;
  comment?: string;
}

export interface TeacherReviewRespDTO {
  id: number;
  teacherId?: number;
  sessionId?: string;
  studentId?: number;
  comment?: string;
  scoreAdjustment?: number;
  isExcellentSample?: boolean;
  isModelMisjudgment?: boolean;
  createTime?: string;
}

// ── Service ──

export const teacherService = {
  // Questions
  pageQuestions(
    params: QuestionPageParams,
  ): Promise<PageInfo<QuestionRespDTO>> {
    return service.get<PageInfo<QuestionRespDTO>>(
      "/ip/v1/questions/page",
      { params },
    );
  },

  createQuestion(data: QuestionCreateDTO): Promise<QuestionRespDTO> {
    return service.post<QuestionRespDTO>(
      "/ip/v1/questions",
      data,
    );
  },

  updateQuestion(
    id: number,
    data: QuestionUpdateDTO,
  ): Promise<QuestionRespDTO> {
    return service.put<QuestionRespDTO>(
      `/ip/v1/questions/${id}`,
      data,
    );
  },

  deleteQuestion(id: number): Promise<void> {
    return service.delete<void>(`/ip/v1/questions/${id}`);
  },

  aiGenerateQuestions(
    params: AiGenerateParams,
  ): Promise<AiGenerateResult> {
    return service.post<AiGenerateResult>(
      "/ip/v1/questions/ai-generate",
      params,
    );
  },

  // Colleges & Majors (reused from questionBankService)
  listColleges(): Promise<CollegeRespDTO[]> {
    return service.get<CollegeRespDTO[]>("/ip/v1/colleges/list");
  },

  listMajors(collegeId?: number): Promise<MajorRespDTO[]> {
    return service.get<MajorRespDTO[]>("/ip/v1/majors/list", {
      params: collegeId ? { collegeId } : undefined,
    });
  },

  // Student reports
  pageStudentReports(
    params: StudentReportPageParams,
  ): Promise<PageInfo<StudentReportDTO>> {
    return service.get<PageInfo<StudentReportDTO>>(
      "/ip/v1/teacher/reports/page",
      { params },
    );
  },

  submitReview(data: ReviewSubmitDTO): Promise<void> {
    return service.post<void>("/ip/v1/teacher/reviews", data);
  },

  // Session reviews (teacher feedback on student interviews)
  getSessionReviews(sessionId: string): Promise<TeacherReviewRespDTO[]> {
    return service.get<TeacherReviewRespDTO[]>(
      `/ip/v1/teacher/sessions/${encodeURIComponent(sessionId)}/reviews`,
    );
  },
};

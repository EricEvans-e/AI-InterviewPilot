import service from "@/lib/request";
import type {
  QuestionRespDTO,
  QuestionPageParams,
  PageInfo,
  CollegeRespDTO,
  MajorRespDTO,
} from "@/services/questionBankService";
import type { InterviewRecordResult } from "@/services/interviewService";

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

export interface ReviewSubmitDTO {
  sessionId: string;
  content: string;
  adjustedScore?: number;
  isExcellentSample?: boolean;
  isModelMisjudge?: boolean;
}

export interface InterviewRecordDTO {
  id: number;
  userId?: number;
  sessionId?: string;
  interviewScore?: number;
  interviewStatus?: string;
  questionCount?: number;
  startTime?: string;
  endTime?: string;
  durationSeconds?: number;
  createTime?: string;
}

export interface TeacherReviewRespDTO {
  id: number;
  teacherId?: number;
  sessionId?: string;
  studentId?: number;
  content?: string;
  adjustedScore?: number;
  isExcellentSample?: boolean;
  isModelMisjudge?: boolean;
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

  pageInterviewRecords(
    params: { pageNum?: number; pageSize?: number },
  ): Promise<PageInfo<InterviewRecordDTO>> {
    return service.get<PageInfo<InterviewRecordDTO>>(
      "/ip/v1/teacher/interview-records",
      { params },
    );
  },

  submitReview(data: ReviewSubmitDTO): Promise<void> {
    const { sessionId, ...body } = data;
    return service.post<void>(
      `/ip/v1/teacher/sessions/${encodeURIComponent(sessionId)}/review`,
      body,
    );
  },

  // Session reviews (teacher feedback on student interviews)
  getSessionReviews(sessionId: string): Promise<TeacherReviewRespDTO[]> {
    return service.get<TeacherReviewRespDTO[]>(
      `/ip/v1/teacher/sessions/${encodeURIComponent(sessionId)}/reviews`,
    );
  },

  // Full report for a single session (teacher view)
  getSessionReport(sessionId: string): Promise<InterviewRecordResult> {
    return service.get<InterviewRecordResult>(
      `/ip/v1/teacher/sessions/${encodeURIComponent(sessionId)}/report`,
    );
  },
};

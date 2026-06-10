import service from "@/lib/request";
import { questionBankService } from "@/services/questionBankService";
import type {
  QuestionRespDTO,
  QuestionPageParams,
  PageInfo,
  CollegeRespDTO,
  MajorRespDTO,
  QuestionCoverageParams,
  QuestionCoverageResult,
} from "@/services/questionBankService";
import type { InterviewRecordResult } from "@/services/interviewService";

// ── Question DTOs ──

export interface QuestionCreateDTO {
  title: string;
  content: string;
  questionType: string;
  collegeId?: number;
  majorId?: number;
  abilityTag?: string;
  difficulty?: string;
  answerTimeSeconds?: number;
  referenceAnswer?: string;
  scoringRule?: string;
  followUpRule?: string;
  followUpQuestions?: string;
  sourceRef?: string;
  year?: number;
  status?: string;
  isAiGenerated?: boolean;
}

export type QuestionUpdateDTO = Partial<QuestionCreateDTO>;
export type QuestionStatus = "draft" | "pending_review" | "approved" | "rejected";

export interface AiGenerateParams {
  collegeId?: number;
  majorId?: number;
  questionType?: string;
  abilityTag?: string;
  count: number;
  difficulty?: string;
  generateFollowUp?: boolean;
  generateScoringRule?: boolean;
  aiType?: string;
  aiPropertiesId?: number;
}

export interface AiGenerateResult {
  questions: QuestionCreateDTO[];
}

export interface AiExpandParams {
  referenceQuestionIds: number[];
  collegeId?: number;
  majorId?: number;
  questionType?: string;
  abilityTag?: string;
  count: number;
  difficulty?: string;
  generateReferenceAnswer?: boolean;
  generateFollowUp?: boolean;
  generateScoringRule?: boolean;
  aiType?: string;
  aiPropertiesId?: number;
}

export interface AiExpandResult {
  questions: QuestionCreateDTO[];
}

export interface QuestionImportItemDTO {
  rowIndex: number;
  valid: boolean;
  errorMessage?: string;
  rawText?: string;
  title?: string;
  content?: string;
  questionType?: string;
  collegeId?: number;
  majorId?: number;
  abilityTag?: string;
  difficulty?: string;
  answerTimeSeconds?: number;
  referenceAnswer?: string;
  scoringRule?: string;
  followUpQuestions?: string;
  sourceRef?: string;
  year?: number;
  warnings?: string[];
}

export interface QuestionImportErrorDTO {
  rowIndex: number;
  message: string;
}

export interface QuestionImportResult {
  batchId: string;
  status: "PARSED" | "PARTIAL_FAILED" | "IMPORTED" | "FAILED";
  totalRows: number;
  validCount: number;
  invalidCount: number;
  importedCount: number;
  items: QuestionImportItemDTO[];
  errors: QuestionImportErrorDTO[];
}

export interface QuestionImportPreviewParams {
  file: File;
  importType: "word_table" | "word_section";
  collegeId?: number;
  majorId?: number;
  defaultQuestionType?: string;
  defaultDifficulty?: string;
  defaultYear?: number;
  statusAfterImport?: "draft" | "pending_review" | "approved" | "rejected";
}

export type {
  QuestionCoverageParams,
  QuestionCoverageResult,
} from "@/services/questionBankService";

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

// ── AI Properties DTOs ──

export interface AiPropertiesDTO {
  id: number;
  aiName: string;
  aiType: string;
  apiKey: string;
  apiUrl: string;
  modelName: string;
  maxTokens: number;
  temperature: number;
  systemPrompt: string;
  isEnabled: number;
  isDefault: number;
  createTime: string;
  updateTime: string;
}

export interface AiPropertiesCreateDTO {
  aiName: string;
  aiType: string;
  apiKey: string;
  apiSecret?: string;
  projectId?: string;
  organizationId?: string;
  apiUrl: string;
  modelName: string;
  maxTokens?: number;
  temperature?: number;
  systemPrompt?: string;
  isEnabled?: number;
  isDefault?: number;
}

export type AiPropertiesUpdateDTO = AiPropertiesCreateDTO & { id: number };

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

  updateQuestionStatus(id: number, status: QuestionStatus): Promise<void> {
    return service.put<void>(`/ip/v1/questions/${id}/status`, null, {
      params: { status },
    });
  },

  async batchUpdateQuestionStatus(ids: number[], status: QuestionStatus): Promise<void> {
    await Promise.all(ids.map((id) => this.updateQuestionStatus(id, status)));
  },

  aiGenerateQuestions(
    params: AiGenerateParams,
  ): Promise<AiGenerateResult> {
    return service.post<AiGenerateResult>(
      "/ip/v1/questions/ai-generate",
      params,
      { timeout: 120_000 },
    );
  },

  aiExpandQuestions(
    params: AiExpandParams,
  ): Promise<AiExpandResult> {
    return service.post<AiExpandResult>(
      "/ip/v1/questions/ai-expand",
      params,
      { timeout: 120_000 },
    );
  },

  previewQuestionImport(
    params: QuestionImportPreviewParams,
  ): Promise<QuestionImportResult> {
    const formData = new FormData();
    formData.set("file", params.file);
    formData.set("importType", params.importType);
    if (params.collegeId !== undefined) formData.set("collegeId", String(params.collegeId));
    if (params.majorId !== undefined) formData.set("majorId", String(params.majorId));
    if (params.defaultQuestionType) formData.set("defaultQuestionType", params.defaultQuestionType);
    if (params.defaultDifficulty) formData.set("defaultDifficulty", params.defaultDifficulty);
    if (params.defaultYear !== undefined) formData.set("defaultYear", String(params.defaultYear));
    if (params.statusAfterImport) formData.set("statusAfterImport", params.statusAfterImport);
    formData.set("dryRun", "true");

    return service.post<QuestionImportResult>(
      "/ip/v1/questions/import",
      formData,
      {
        timeout: 60_000,
        headers: { "Content-Type": "multipart/form-data" },
      },
    );
  },

  confirmQuestionImport(batchId: string): Promise<QuestionImportResult> {
    return service.post<QuestionImportResult>(
      `/ip/v1/questions/import/${encodeURIComponent(batchId)}/confirm`,
    );
  },

  getQuestionCoverage(
    params: QuestionCoverageParams,
  ): Promise<QuestionCoverageResult> {
    return questionBankService.getQuestionCoverage(params);
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

  deleteInterviewRecord(sessionId: string): Promise<void> {
    return service.delete<void>(
      `/ip/v1/teacher/sessions/${encodeURIComponent(sessionId)}/record`,
    );
  },

  // ── AI Properties ──
  getEnabledAiProperties(): Promise<AiPropertiesDTO[]> {
    return service.get<AiPropertiesDTO[]>("/ip/v1/ai-properties/enabled");
  },

  pageAiProperties(params: {
    current?: number;
    size?: number;
    aiName?: string;
    aiType?: string;
    isEnabled?: number;
  }): Promise<{ records: AiPropertiesDTO[]; total: number; current: number; size: number }> {
    return service.get("/ip/v1/ai-properties", { params });
  },

  getAiPropertiesById(id: number): Promise<AiPropertiesDTO> {
    return service.get<AiPropertiesDTO>(`/ip/v1/ai-properties/${id}`);
  },

  createAiProperties(data: AiPropertiesCreateDTO): Promise<void> {
    return service.post<void>("/ip/v1/ai-properties", data);
  },

  updateAiProperties(data: AiPropertiesUpdateDTO): Promise<void> {
    return service.put<void>("/ip/v1/ai-properties", data);
  },

  deleteAiProperties(id: number): Promise<void> {
    return service.delete<void>(`/ip/v1/ai-properties/${id}`);
  },

  toggleAiPropertiesStatus(id: number, isEnabled: number): Promise<void> {
    return service.put<void>(`/ip/v1/ai-properties/${id}/status`, null, {
      params: { isEnabled },
    });
  },

  setDefaultAiProperties(id: number): Promise<void> {
    return service.put<void>(`/ip/v1/ai-properties/${id}/default`);
  },
};

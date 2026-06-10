import service from "@/lib/request";

export interface CollegeRespDTO {
  id: number;
  name: string;
  code?: string;
  type?: string;
  province?: string;
  city?: string;
  level?: string;
  officialUrl?: string;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

export interface MajorRespDTO {
  id: number;
  collegeId?: number;
  name: string;
  code?: string;
  category?: string;
  targetType?: string;
  testForm?: string;
  testContent?: string;
  scoreStructure?: string;
  year?: number;
  officialUrl?: string;
  createTime?: string;
  updateTime?: string;
}

export interface QuestionRespDTO {
  id: number;
  title: string;
  content?: string;
  questionType?: string;
  collegeId?: number;
  collegeName?: string;
  majorId?: number;
  majorName?: string;
  abilityTag?: string;
  difficulty?: string;
  answerTimeSeconds?: number;
  referenceAnswer?: string;
  scoringRule?: string;
  followUpRule?: string;
  followUpQuestions?: string;
  sourceRef?: string;
  isAiGenerated?: boolean;
  status?: string;
  creatorId?: number;
  year?: number;
  createTime?: string;
  updateTime?: string;
}

export interface QuestionPageParams {
  collegeId?: number;
  majorId?: number;
  titleKeyword?: string;
  questionType?: string;
  abilityTag?: string;
  difficulty?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface PageInfo<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface QuestionCoverageParams {
  collegeId?: number;
  majorId?: number;
  interviewMode?: string;
  requiredCount?: number;
  abilityTag?: string;
  difficulty?: string;
}

export interface QuestionCoverageResult {
  collegeId?: number;
  majorId?: number;
  interviewMode?: string;
  approvedCount: number;
  exactMatchCount: number;
  fallbackCount: number;
  canStartImmediately: boolean;
  mayNeedAiGeneration: boolean;
}

export const QUESTION_TYPE_OPTIONS = [
  { value: "综合题", label: "综合题" },
  { value: "专业题", label: "专业题" },
  { value: "其他题", label: "其他题" },
] as const;

export const DIFFICULTY_OPTIONS = [
  { value: "easy", label: "简单", color: "bg-green-100 text-green-700" },
  { value: "medium", label: "中等", color: "bg-yellow-100 text-yellow-700" },
  { value: "hard", label: "困难", color: "bg-red-100 text-red-700" },
  { value: "pressure", label: "高压", color: "bg-purple-100 text-purple-700" },
] as const;

export const ABILITY_TAG_OPTIONS = [
  { value: "logical_thinking", label: "逻辑思维" },
  { value: "communication", label: "语言表达" },
  { value: "professional_knowledge", label: "专业知识" },
  { value: "problem_solving", label: "问题解决" },
  { value: "stress_tolerance", label: "抗压能力" },
  { value: "teamwork", label: "团队协作" },
  { value: "innovation", label: "创新能力" },
  { value: "self_awareness", label: "自我认知" },
] as const;

export const questionBankService = {
  async pageQuestions(params: QuestionPageParams): Promise<PageInfo<QuestionRespDTO>> {
    return service.get<PageInfo<QuestionRespDTO>>(
      "/ip/v1/questions/page",
      { params },
    );
  },

  async listColleges(): Promise<CollegeRespDTO[]> {
    return service.get<CollegeRespDTO[]>("/ip/v1/colleges/list");
  },

  async listMajors(collegeId?: number): Promise<MajorRespDTO[]> {
    return service.get<MajorRespDTO[]>("/ip/v1/majors/list", {
      params: collegeId ? { collegeId } : undefined,
    });
  },

  async getQuestionCoverage(
    params: QuestionCoverageParams,
  ): Promise<QuestionCoverageResult> {
    return service.get<QuestionCoverageResult>("/ip/v1/questions/coverage", {
      params,
    });
  },
};

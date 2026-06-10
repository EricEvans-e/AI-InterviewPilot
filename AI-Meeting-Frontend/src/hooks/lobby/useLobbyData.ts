import { useCallback, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  questionBankService,
  type QuestionPageParams,
  type CollegeRespDTO,
  type MajorRespDTO,
  type QuestionRespDTO,
  type QuestionCoverageResult,
  type PageInfo,
} from "@/services/questionBankService";

export interface LobbyFilters {
  collegeId?: number;
  majorId?: number;
  questionTypes: string[];
  abilityTags: string[];
  difficulties: string[];
}

const EMPTY_FILTERS: LobbyFilters = {
  questionTypes: [],
  abilityTags: [],
  difficulties: [],
};

const QUESTION_PAGE_SIZE = 20;

export function useLobbyData(interviewMode = "综合题", requiredCount = 5) {
  const [filters, setFilters] = useState<LobbyFilters>(EMPTY_FILTERS);
  const [pageNum, setPageNum] = useState(1);
  const coverageInterviewMode =
    filters.questionTypes.length === 1 ? filters.questionTypes[0] : interviewMode;

  const collegesQuery = useQuery<CollegeRespDTO[]>({
    queryKey: ["lobby", "colleges"],
    queryFn: () => questionBankService.listColleges(),
    staleTime: 5 * 60 * 1000,
  });

  const majorsQuery = useQuery<MajorRespDTO[]>({
    queryKey: ["lobby", "majors", filters.collegeId],
    queryFn: () => questionBankService.listMajors(filters.collegeId),
    staleTime: 5 * 60 * 1000,
  });

  const buildQuestionParams = useCallback((): QuestionPageParams => {
    const params: QuestionPageParams = {
      pageNum,
      pageSize: QUESTION_PAGE_SIZE,
      status: "approved",
    };
    if (filters.collegeId) {
      params.collegeId = filters.collegeId;
    }
    if (filters.majorId) {
      params.majorId = filters.majorId;
    }
    if (filters.questionTypes.length === 1) {
      params.questionType = filters.questionTypes[0];
    }
    if (filters.abilityTags.length === 1) {
      params.abilityTag = filters.abilityTags[0];
    }
    if (filters.difficulties.length === 1) {
      params.difficulty = filters.difficulties[0];
    }
    return params;
  }, [filters, pageNum]);

  const questionsQuery = useQuery<PageInfo<QuestionRespDTO>>({
    queryKey: ["lobby", "questions", filters, pageNum],
    queryFn: () => questionBankService.pageQuestions(buildQuestionParams()),
  });

  const coverageQuery = useQuery<QuestionCoverageResult>({
    queryKey: [
      "lobby",
      "coverage",
      filters.collegeId,
      filters.majorId,
      coverageInterviewMode,
      filters.abilityTags,
      filters.difficulties,
      requiredCount,
    ],
    queryFn: () =>
      questionBankService.getQuestionCoverage({
        collegeId: filters.collegeId,
        majorId: filters.majorId,
        interviewMode: coverageInterviewMode,
        requiredCount,
        abilityTag:
          filters.abilityTags.length === 1 ? filters.abilityTags[0] : undefined,
        difficulty:
          filters.difficulties.length === 1 ? filters.difficulties[0] : undefined,
      }),
  });

  const updateFilters = useCallback((patch: Partial<LobbyFilters>) => {
    setFilters((prev) => ({ ...prev, ...patch }));
    setPageNum(1);
  }, []);

  const resetFilters = useCallback(() => {
    setFilters(EMPTY_FILTERS);
    setPageNum(1);
  }, []);

  const toggleArrayFilter = useCallback(
    (key: "questionTypes" | "abilityTags" | "difficulties", value: string) => {
      setFilters((prev) => {
        const current = prev[key];
        const next = current.includes(value)
          ? current.filter((v) => v !== value)
          : [...current, value];
        return { ...prev, [key]: next };
      });
      setPageNum(1);
    },
    [],
  );

  return {
    filters,
    colleges: collegesQuery.data ?? [],
    collegesLoading: collegesQuery.isLoading,
    majors: majorsQuery.data ?? [],
    majorsLoading: majorsQuery.isLoading,
    questions: questionsQuery.data?.records ?? [],
    totalQuestions: questionsQuery.data?.total ?? 0,
    totalPages: questionsQuery.data?.pages ?? 0,
    questionsLoading: questionsQuery.isLoading,
    coverage: coverageQuery.data,
    coverageInterviewMode,
    coverageLoading: coverageQuery.isLoading,
    coverageError: coverageQuery.error,
    pageNum,
    setPageNum,
    updateFilters,
    resetFilters,
    toggleArrayFilter,
  };
}

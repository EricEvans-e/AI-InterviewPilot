import { useState, useCallback } from "react";
import {
  useQuery,
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";
import { collegeService } from "@/services/collegeService";
import type {
  CollegeCreateDTO,
  CollegePageParams,
  MajorCreateDTO,
  MajorPageParams,
  ExamOutlineCreateDTO,
  ExamOutlinePageParams,
} from "@/services/collegeService";

const COLLEGES_QUERY_KEY = "college-manage-colleges";
const MAJORS_QUERY_KEY = "college-manage-majors";
const EXAM_OUTLINES_QUERY_KEY = "college-manage-exam-outlines";
const PAGE_SIZE = 15;

// ── Colleges Hook ──

export function useColleges() {
  const queryClient = useQueryClient();

  const [filters, setFilters] = useState<CollegePageParams>({
    pageNum: 1,
    pageSize: PAGE_SIZE,
  });

  const { data, isLoading, isFetching } = useQuery({
    queryKey: [COLLEGES_QUERY_KEY, filters],
    queryFn: () => collegeService.pageColleges(filters),
  });

  const colleges = data?.records ?? [];
  const total = data?.total ?? 0;
  const totalPages = data?.pages ?? 0;

  const createMutation = useMutation({
    mutationFn: (data: CollegeCreateDTO) =>
      collegeService.createCollege(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [COLLEGES_QUERY_KEY] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<CollegeCreateDTO> }) =>
      collegeService.updateCollege(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [COLLEGES_QUERY_KEY] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => collegeService.deleteCollege(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [COLLEGES_QUERY_KEY] });
    },
  });

  const setPage = useCallback((page: number) => {
    setFilters((prev) => ({ ...prev, pageNum: page }));
  }, []);

  const updateFilters = useCallback(
    (patch: Partial<CollegePageParams>) => {
      setFilters((prev) => ({ ...prev, ...patch, pageNum: 1 }));
    },
    [],
  );

  return {
    colleges,
    total,
    totalPages,
    currentPage: filters.pageNum ?? 1,
    pageSize: PAGE_SIZE,
    filters,
    isLoading,
    isFetching,
    setPage,
    updateFilters,
    createCollege: createMutation,
    updateCollege: updateMutation,
    deleteCollege: deleteMutation,
  };
}

// ── Majors Hook ──

export function useMajors() {
  const queryClient = useQueryClient();

  const [filters, setFilters] = useState<MajorPageParams>({
    pageNum: 1,
    pageSize: PAGE_SIZE,
  });

  const { data, isLoading, isFetching } = useQuery({
    queryKey: [MAJORS_QUERY_KEY, filters],
    queryFn: () => collegeService.pageMajors(filters),
  });

  const majors = data?.records ?? [];
  const total = data?.total ?? 0;
  const totalPages = data?.pages ?? 0;

  const createMutation = useMutation({
    mutationFn: (data: MajorCreateDTO) =>
      collegeService.createMajor(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [MAJORS_QUERY_KEY] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<MajorCreateDTO> }) =>
      collegeService.updateMajor(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [MAJORS_QUERY_KEY] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => collegeService.deleteMajor(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [MAJORS_QUERY_KEY] });
    },
  });

  const setPage = useCallback((page: number) => {
    setFilters((prev) => ({ ...prev, pageNum: page }));
  }, []);

  const updateFilters = useCallback(
    (patch: Partial<MajorPageParams>) => {
      setFilters((prev) => ({ ...prev, ...patch, pageNum: 1 }));
    },
    [],
  );

  return {
    majors,
    total,
    totalPages,
    currentPage: filters.pageNum ?? 1,
    pageSize: PAGE_SIZE,
    filters,
    isLoading,
    isFetching,
    setPage,
    updateFilters,
    createMajor: createMutation,
    updateMajor: updateMutation,
    deleteMajor: deleteMutation,
  };
}

// ── Exam Outlines Hook ──

export function useExamOutlines() {
  const queryClient = useQueryClient();

  const [filters, setFilters] = useState<ExamOutlinePageParams>({
    pageNum: 1,
    pageSize: PAGE_SIZE,
  });

  const { data, isLoading, isFetching } = useQuery({
    queryKey: [EXAM_OUTLINES_QUERY_KEY, filters],
    queryFn: () => collegeService.pageExamOutlines(filters),
  });

  const examOutlines = data?.records ?? [];
  const total = data?.total ?? 0;
  const totalPages = data?.pages ?? 0;

  const createMutation = useMutation({
    mutationFn: (data: ExamOutlineCreateDTO) =>
      collegeService.createExamOutline(data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: [EXAM_OUTLINES_QUERY_KEY],
      });
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: number;
      data: Partial<ExamOutlineCreateDTO>;
    }) => collegeService.updateExamOutline(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: [EXAM_OUTLINES_QUERY_KEY],
      });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => collegeService.deleteExamOutline(id),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: [EXAM_OUTLINES_QUERY_KEY],
      });
    },
  });

  const setPage = useCallback((page: number) => {
    setFilters((prev) => ({ ...prev, pageNum: page }));
  }, []);

  const updateFilters = useCallback(
    (patch: Partial<ExamOutlinePageParams>) => {
      setFilters((prev) => ({ ...prev, ...patch, pageNum: 1 }));
    },
    [],
  );

  return {
    examOutlines,
    total,
    totalPages,
    currentPage: filters.pageNum ?? 1,
    pageSize: PAGE_SIZE,
    filters,
    isLoading,
    isFetching,
    setPage,
    updateFilters,
    createExamOutline: createMutation,
    updateExamOutline: updateMutation,
    deleteExamOutline: deleteMutation,
  };
}

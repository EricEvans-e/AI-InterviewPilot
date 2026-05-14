import { useMutation, useQuery } from "@tanstack/react-query";
import {
  studentService,
  type StudentProfileSaveReqDTO,
} from "@/services/studentService";

const STUDENT_PROFILE_QUERY_KEY = ["studentProfile"] as const;

export function useStudentProfile() {
  const query = useQuery({
    queryKey: STUDENT_PROFILE_QUERY_KEY,
    queryFn: studentService.getProfile,
  });

  const mutation = useMutation({
    mutationFn: (data: StudentProfileSaveReqDTO) =>
      studentService.saveProfile(data),
    onSuccess: () => query.refetch(),
  });

  return {
    profile: query.data,
    isLoading: query.isLoading,
    error: query.error,
    saveProfile: mutation.mutate,
    saveProfileAsync: mutation.mutateAsync,
    isSaving: mutation.isPending,
    saveError: mutation.error,
    resetSave: mutation.reset,
  };
}

import service from "@/lib/request";

export interface StudentCollegeDTO {
  id: number;
  name: string;
}

export interface StudentMajorDTO {
  id: number;
  name: string;
}

export interface StudentProfileRespDTO {
  id?: number;
  userId?: number;
  schoolName?: string;
  grade?: string;
  examCategory?: string;
  trainingStage?: string;
  targetColleges?: StudentCollegeDTO[];
  targetMajors?: StudentMajorDTO[];
}

export interface StudentProfileSaveReqDTO {
  schoolName?: string;
  grade?: string;
  examCategory?: string;
  trainingStage?: string;
  collegeIds?: number[];
  majorIds?: number[];
}

export const studentService = {
  getProfile(): Promise<StudentProfileRespDTO> {
    return service.get<StudentProfileRespDTO>("/ip/v1/student/profile");
  },

  saveProfile(data: StudentProfileSaveReqDTO): Promise<void> {
    return service.put<void, StudentProfileSaveReqDTO>(
      "/ip/v1/student/profile",
      data,
    );
  },
};

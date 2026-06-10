import { afterEach, describe, expect, it, vi } from "vitest";
import requestService from "@/lib/request";
import { teacherService } from "@/services/teacherService";
import { questionBankService } from "@/services/questionBankService";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("teacherService.deleteInterviewRecord", () => {
  it("deletes one teacher interview record by session id", async () => {
    const deleteSpy = vi.spyOn(requestService, "delete").mockResolvedValueOnce(undefined);

    await teacherService.deleteInterviewRecord("session-1");

    expect(deleteSpy).toHaveBeenCalledTimes(1);
    expect(deleteSpy).toHaveBeenCalledWith("/ip/v1/teacher/sessions/session-1/record");
  });

  it("encodes session id in the delete path", async () => {
    const deleteSpy = vi.spyOn(requestService, "delete").mockResolvedValueOnce(undefined);

    await teacherService.deleteInterviewRecord("session/a b");

    expect(deleteSpy).toHaveBeenCalledWith("/ip/v1/teacher/sessions/session%2Fa%20b/record");
  });
});

describe("teacherService question import", () => {
  it("previews a Word import with multipart form data", async () => {
    const postSpy = vi.spyOn(requestService, "post").mockResolvedValueOnce({
      batchId: "imp-1",
      status: "PARSED",
      totalRows: 1,
      validCount: 1,
      invalidCount: 0,
      importedCount: 0,
      items: [],
      errors: [],
    });
    const file = new File(["docx"], "questions.docx", {
      type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    });

    await teacherService.previewQuestionImport({
      file,
      importType: "word_table",
      collegeId: 11,
      majorId: 22,
      defaultQuestionType: "专业题",
      defaultDifficulty: "medium",
      defaultYear: 2026,
      statusAfterImport: "pending_review",
    });

    expect(postSpy).toHaveBeenCalledTimes(1);
    expect(postSpy).toHaveBeenCalledWith(
      "/ip/v1/questions/import",
      expect.any(FormData),
      expect.objectContaining({
        timeout: 60_000,
        headers: { "Content-Type": "multipart/form-data" },
      }),
    );
    const formData = postSpy.mock.calls[0][1] as FormData;
    expect(formData.get("file")).toBe(file);
    expect(formData.get("importType")).toBe("word_table");
    expect(formData.get("collegeId")).toBe("11");
    expect(formData.get("majorId")).toBe("22");
    expect(formData.get("defaultQuestionType")).toBe("专业题");
  });

  it("confirms a parsed import batch", async () => {
    const postSpy = vi.spyOn(requestService, "post").mockResolvedValueOnce({
      batchId: "imp-1",
      status: "IMPORTED",
      importedCount: 2,
      totalRows: 2,
      validCount: 2,
      invalidCount: 0,
      items: [],
      errors: [],
    });

    await teacherService.confirmQuestionImport("imp-1");

    expect(postSpy).toHaveBeenCalledWith("/ip/v1/questions/import/imp-1/confirm");
  });

  it("loads question-bank coverage", async () => {
    const coverageSpy = vi.spyOn(questionBankService, "getQuestionCoverage").mockResolvedValueOnce({
      collegeId: 11,
      majorId: 22,
      interviewMode: "专业题",
      approvedCount: 3,
      exactMatchCount: 1,
      fallbackCount: 2,
      canStartImmediately: true,
      mayNeedAiGeneration: true,
    });

    await teacherService.getQuestionCoverage({
      collegeId: 11,
      majorId: 22,
      interviewMode: "专业题",
      requiredCount: 5,
    });

    expect(coverageSpy).toHaveBeenCalledWith({
      collegeId: 11,
      majorId: 22,
      interviewMode: "专业题",
      requiredCount: 5,
    });
  });
});

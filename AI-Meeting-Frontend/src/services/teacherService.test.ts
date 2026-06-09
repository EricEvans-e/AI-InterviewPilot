import { afterEach, describe, expect, it, vi } from "vitest";
import requestService from "@/lib/request";
import { teacherService } from "@/services/teacherService";

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

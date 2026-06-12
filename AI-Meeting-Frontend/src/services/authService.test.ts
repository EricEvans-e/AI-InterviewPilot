import { afterEach, describe, expect, it, vi } from "vitest";
import requestService from "@/lib/request";
import { authService } from "@/services/authService";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("authService.changePassword", () => {
  it("submits current and new password payload to the password endpoint", async () => {
    const putSpy = vi.spyOn(requestService, "put").mockResolvedValueOnce(undefined);

    await authService.changePassword({
      oldPassword: "old-pass",
      newPassword: "new-pass-123",
      confirmPassword: "new-pass-123",
    });

    expect(putSpy).toHaveBeenCalledWith("/ip/v1/users/password", {
      oldPassword: "old-pass",
      newPassword: "new-pass-123",
      confirmPassword: "new-pass-123",
    });
  });
});

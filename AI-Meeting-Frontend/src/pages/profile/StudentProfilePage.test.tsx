import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import StudentProfilePage from "@/pages/profile/StudentProfilePage";
import { useStudentProfile } from "@/hooks/profile/useStudentProfile";
import { authService } from "@/services/authService";

vi.mock("@/hooks/profile/useStudentProfile", () => ({
  useStudentProfile: vi.fn(),
}));

vi.mock("@/services/authService", () => ({
  authService: {
    changePassword: vi.fn(),
  },
}));

const renderPage = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <StudentProfilePage />
    </QueryClientProvider>,
  );
};

describe("StudentProfilePage password change", () => {
  beforeEach(() => {
    vi.mocked(useStudentProfile).mockReturnValue({
      profile: {
        schoolName: "",
        targetColleges: [],
        targetMajors: [],
      },
      isLoading: false,
      error: null,
      saveProfile: vi.fn(),
      saveProfileAsync: vi.fn(),
      isSaving: false,
      saveError: null,
      resetSave: vi.fn(),
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("submits password change when confirmation matches", async () => {
    vi.mocked(authService.changePassword).mockResolvedValueOnce(undefined);

    renderPage();

    fireEvent.change(screen.getByLabelText("当前密码"), {
      target: { value: "old-pass" },
    });
    fireEvent.change(screen.getByLabelText("新密码"), {
      target: { value: "new-pass-123" },
    });
    fireEvent.change(screen.getByLabelText("确认新密码"), {
      target: { value: "new-pass-123" },
    });

    fireEvent.click(screen.getByRole("button", { name: "修改密码" }));

    await waitFor(() => {
      expect(authService.changePassword).toHaveBeenCalledWith({
        oldPassword: "old-pass",
        newPassword: "new-pass-123",
        confirmPassword: "new-pass-123",
      });
    });

    expect(await screen.findByText("密码修改成功")).toBeTruthy();
  });

  it("blocks submission when confirmation does not match", async () => {
    renderPage();

    fireEvent.change(screen.getByLabelText("当前密码"), {
      target: { value: "old-pass" },
    });
    fireEvent.change(screen.getByLabelText("新密码"), {
      target: { value: "new-pass-123" },
    });
    fireEvent.change(screen.getByLabelText("确认新密码"), {
      target: { value: "wrong-confirm" },
    });

    fireEvent.click(screen.getByRole("button", { name: "修改密码" }));

    expect(authService.changePassword).not.toHaveBeenCalled();
    expect(await screen.findByText("两次输入的新密码不一致")).toBeTruthy();
  });
});

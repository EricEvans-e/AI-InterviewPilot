import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AdminAgentConfigPage from "@/pages/admin/AdminAgentConfigPage";
import { adminService } from "@/services/adminService";

vi.mock("@/services/adminService", () => ({
  adminService: {
    getSceneBindings: vi.fn(),
    switchSceneAgent: vi.fn(),
  },
}));

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AdminAgentConfigPage />
    </QueryClientProvider>,
  );
}

function openSelect(select: HTMLElement) {
  fireEvent.pointerDown(select, {
    button: 0,
    ctrlKey: false,
    pointerType: "mouse",
  });
}

describe("AdminAgentConfigPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Element.prototype.scrollIntoView = vi.fn();
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
    vi.mocked(adminService.switchSceneAgent).mockResolvedValue(undefined);
  });

  it("shows Pro choices only for text-only scenes", async () => {
    vi.mocked(adminService.getSceneBindings).mockResolvedValue([
      {
        sceneCode: "interview-question-extraction",
        sceneName: "面试出题官",
        activeAgentId: 8,
        activeAgentName: "Mimo 2.5 面试出题官",
        activeProvider: "openai",
        candidates: [
          { id: 8, agentName: "Mimo 2.5 面试出题官", aiProvider: "openai" },
        ],
      },
      {
        sceneCode: "interview-answer-evaluation",
        sceneName: "用户答案评分官",
        activeAgentId: 11,
        activeAgentName: "Mimo 2.5 答案评分官",
        activeProvider: "openai",
        candidates: [
          { id: 11, agentName: "Mimo 2.5 答案评分官", aiProvider: "openai" },
          { id: 14, agentName: "Mimo 2.5 Pro 答案评分官", aiProvider: "openai" },
        ],
      },
      {
        sceneCode: "interview-demeanor",
        sceneName: "神态分析官",
        activeAgentId: 9,
        activeAgentName: "Mimo 2.5 神态分析官",
        activeProvider: "openai",
        candidates: [
          { id: 9, agentName: "Mimo 2.5 神态分析官", aiProvider: "openai" },
        ],
      },
    ]);

    renderPage();

    expect(await screen.findAllByText("Mimo 2.5 面试出题官")).not.toHaveLength(0);
    expect(screen.getAllByText("Mimo 2.5 答案评分官")).not.toHaveLength(0);
    expect(screen.getAllByText("Mimo 2.5 神态分析官")).not.toHaveLength(0);

    const selects = screen.getAllByRole("combobox");
    openSelect(selects[1]!);

    expect(
      await screen.findByRole("option", { name: /Mimo 2.5 Pro 答案评分官/ }),
    ).toBeTruthy();
    expect(screen.queryByText("Mimo 2.5 Pro 面试出题官")).toBeNull();
    expect(screen.queryByText("Mimo 2.5 Pro 神态分析官")).toBeNull();
  });

  it("switches the selected scene agent through the admin API", async () => {
    vi.mocked(adminService.getSceneBindings).mockResolvedValue([
      {
        sceneCode: "interview-answer-evaluation",
        sceneName: "用户答案评分官",
        activeAgentId: 11,
        activeAgentName: "Mimo 2.5 答案评分官",
        activeProvider: "openai",
        candidates: [
          { id: 11, agentName: "Mimo 2.5 答案评分官", aiProvider: "openai" },
          { id: 14, agentName: "Mimo 2.5 Pro 答案评分官", aiProvider: "openai" },
        ],
      },
    ]);

    renderPage();

    openSelect(await screen.findByRole("combobox"));
    fireEvent.click(await screen.findByRole("option", { name: /Mimo 2.5 Pro 答案评分官/ }));

    await waitFor(() =>
      expect(adminService.switchSceneAgent).toHaveBeenCalledWith(
        "interview-answer-evaluation",
        14,
      ),
    );
  });
});

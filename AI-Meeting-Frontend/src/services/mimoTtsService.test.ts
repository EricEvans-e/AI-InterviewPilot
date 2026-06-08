import { describe, expect, it, vi } from "vitest";

vi.mock("@/lib/request", () => ({
  default: {
    post: vi.fn(async () => ({
      taskStatus: "5",
      code: 0,
      audioBase64: "UklGRg==",
      completed: true,
      success: true,
    })),
    get: vi.fn(),
  },
}));

import service from "@/lib/request";
import { mimoTtsService, normalizeTaskResult } from "@/services/mimoTtsService";

describe("mimoTtsService", () => {
  it("treats numeric task status and string code as a completed successful task", () => {
    const result = normalizeTaskResult({
      taskStatus: 5 as unknown as string,
      code: "0" as unknown as number,
      message: "ok",
    });

    expect(result.taskStatus).toBe("5");
    expect(result.code).toBe(0);
    expect(result.completed).toBe(true);
    expect(result.success).toBe(true);
  });

  it("maps pybuf fields into playable audio fields for compatibility", () => {
    const result = normalizeTaskResult({
      taskStatus: "5",
      pybufContent: "QQ==",
      pybufUrl: "https://example.com/audio.mp3",
    });

    expect(result.audioBase64).toBe("QQ==");
    expect(result.audioUrl).toBe("https://example.com/audio.mp3");
    expect(result.completed).toBe(true);
    expect(result.success).toBe(true);
  });

  it("posts synthesis requests to the Mimo endpoint", async () => {
    await mimoTtsService.synthesize({ text: "hello" });

    expect(service.post).toHaveBeenCalledWith(
      "/ip/v1/mimo/tts/synthesize",
      { text: "hello" },
      { signal: undefined },
    );
  });
});

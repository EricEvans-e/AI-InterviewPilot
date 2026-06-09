import { act, renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAudioTranscriptionTransport } from "@/hooks/audio/useAudioTranscriptionTransport";

const { instances, MockAudioToTextWebSocket } = vi.hoisted(() => {
  const instances: Array<{
    userId: string;
    onTranscription?: (text: string) => void;
    onFinal?: (text: string) => void;
    onError?: (message: string) => void;
    onConnected?: () => void;
    onDisconnected?: () => void;
    onSocketOpen?: () => void;
    connect: ReturnType<typeof vi.fn>;
    disconnect: ReturnType<typeof vi.fn>;
    sendAudio: ReturnType<typeof vi.fn>;
    sendCommand: ReturnType<typeof vi.fn>;
  }> = [];

  class MockAudioToTextWebSocket {
    public onTranscription?: (text: string) => void;
    public onFinal?: (text: string) => void;
    public onError?: (message: string) => void;
    public onConnected?: () => void;
    public onDisconnected?: () => void;
    public onSocketOpen?: () => void;

    public connect = vi.fn();
    public disconnect = vi.fn();
    public sendAudio = vi.fn();
    public sendCommand = vi.fn();
    public userId: string;

    constructor(userId: string) {
      this.userId = userId;
      instances.push(this);
    }
  }

  return {
    instances,
    MockAudioToTextWebSocket,
  };
});

vi.mock("@/services/audioToTextWs", () => ({
  AudioToTextWebSocket: MockAudioToTextWebSocket,
}));

describe("useAudioTranscriptionTransport", () => {
  beforeEach(() => {
    instances.length = 0;
    vi.clearAllMocks();
  });

  it("starts transcription as soon as the socket opens", () => {
    const { result } = renderHook(() =>
      useAudioTranscriptionTransport({
        userId: "tester",
        onReplace: vi.fn(),
        onArchive: vi.fn(),
        onError: vi.fn(),
      }),
    );

    act(() => {
      result.current.connect();
    });

    const transport = instances.at(-1);
    expect(transport).toBeDefined();

    act(() => {
      transport?.onSocketOpen?.();
    });

    expect(transport?.sendCommand).toHaveBeenCalledWith("start_transcription");
  });

  it("waits for the final transcription packet before disconnecting on stop", async () => {
    const { result } = renderHook(() =>
      useAudioTranscriptionTransport({
        userId: "tester",
        onReplace: vi.fn(),
        onArchive: vi.fn(),
        onError: vi.fn(),
      }),
    );

    act(() => {
      result.current.connect();
    });

    const transport = instances.at(-1);
    expect(transport).toBeDefined();

    let stopPromise: Promise<void> | undefined;
    act(() => {
      stopPromise = result.current.stop();
    });

    expect(transport?.sendCommand).toHaveBeenCalledWith("stop_transcription");
    expect(transport?.disconnect).not.toHaveBeenCalled();

    act(() => {
      transport?.onFinal?.("final answer");
    });

    await act(async () => {
      await stopPromise;
    });

    expect(transport?.disconnect).toHaveBeenCalledTimes(1);
  });
});

import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CHAT_MESSAGE_STATUS, type ChatMessage } from "@/lib/chat";
import { useChatTtsPlayback } from "@/hooks/audio/useChatTtsPlayback";

const playbackState = vi.hoisted(() => ({
  getCachedObjectUrl: vi.fn(),
  getPreparedAudioKey: vi.fn(),
  cacheObjectUrl: vi.fn(),
  removeCachedObjectUrl: vi.fn(),
  resolvePlayableAudioUrl: vi.fn(),
  revokePreparedObjectUrls: vi.fn(),
  resetAudioElement: vi.fn(),
  primePlaybackFromGesture: vi.fn(),
  playObjectUrl: vi.fn(),
  disposeAudioElement: vi.fn(),
  audioRef: { current: { pause: vi.fn() } },
  synthesize: vi.fn(),
}));

vi.mock("@/hooks/audio/useChatTtsAudioCache", () => ({
  useChatTtsAudioCache: () => ({
    getCachedObjectUrl: playbackState.getCachedObjectUrl,
    getPreparedAudioKey: playbackState.getPreparedAudioKey,
    cacheObjectUrl: playbackState.cacheObjectUrl,
    removeCachedObjectUrl: playbackState.removeCachedObjectUrl,
    resolvePlayableAudioUrl: playbackState.resolvePlayableAudioUrl,
    revokePreparedObjectUrls: playbackState.revokePreparedObjectUrls,
  }),
}));

vi.mock("@/hooks/audio/useChatTtsAudioElement", () => ({
  useChatTtsAudioElement: () => ({
    audioRef: playbackState.audioRef,
    resetAudioElement: playbackState.resetAudioElement,
    primePlaybackFromGesture: playbackState.primePlaybackFromGesture,
    playObjectUrl: playbackState.playObjectUrl,
    disposeAudioElement: playbackState.disposeAudioElement,
  }),
}));

vi.mock("@/services/mimoTtsService", () => ({
  mimoTtsService: {
    synthesize: (...args: unknown[]) => playbackState.synthesize(...args),
  },
}));

const createQuestionMessage = (): ChatMessage => ({
  id: "message-1",
  role: "assistant",
  content: "请介绍一下你的项目经验",
  timestamp: 1,
  status: CHAT_MESSAGE_STATUS.done,
  tts: {
    text: "请介绍一下你的项目经验",
    autoPlay: true,
    cacheKey: "q1",
  },
});

describe("useChatTtsPlayback", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    playbackState.getCachedObjectUrl.mockReturnValue(undefined);
    playbackState.getPreparedAudioKey.mockImplementation(
      (message: ChatMessage) => message.tts?.cacheKey?.trim() || message.id,
    );
    playbackState.playObjectUrl.mockResolvedValue(undefined);
    playbackState.primePlaybackFromGesture.mockResolvedValue(undefined);
    playbackState.resolvePlayableAudioUrl.mockResolvedValue("blob:generated");
    playbackState.synthesize.mockResolvedValue({
      audioBase64: "QQ==",
      audioUrl: null,
    });
  });

  it("reuses cached audio for manual playback instead of forcing a new synthesis", async () => {
    const message = createQuestionMessage();
    playbackState.getCachedObjectUrl.mockReturnValue("blob:cached");

    const { result } = renderHook(() => useChatTtsPlayback([message]));

    act(() => {
      result.current.toggleMessagePlayback(message);
    });

    await waitFor(() => {
      expect(playbackState.primePlaybackFromGesture).toHaveBeenCalledTimes(1);
      expect(playbackState.playObjectUrl).toHaveBeenCalledWith("blob:cached");
    });

    expect(playbackState.synthesize).not.toHaveBeenCalled();
    expect(playbackState.removeCachedObjectUrl).not.toHaveBeenCalled();
  });
});

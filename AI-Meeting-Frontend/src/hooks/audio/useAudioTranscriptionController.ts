import {
  useCallback,
  useEffect,
  useMemo,
  useReducer,
  useRef,
  useState,
} from "react";
import {
  createInitialAudioTranscriptionState,
  getMergedAudioTranscription,
  reduceAudioTranscriptionState,
} from "@/lib/audioTranscription";
import { useAudioTranscriptionTransport } from "@/hooks/audio/useAudioTranscriptionTransport";
import { useMicrophonePcmStream } from "@/hooks/audio/useMicrophonePcmStream";
import type { UserRespDTO } from "@/types/auth";

const AUDIO_SAMPLE_RATE = 16000;
const START_RECORDING_ERROR =
  "Unable to access microphone or connect to transcription";

const resolveAudioUserId = (currentUser: UserRespDTO | null) => {
  const normalizedUsername = currentUser?.username?.trim();
  const normalizedUserId =
    typeof currentUser?.id === "number" && currentUser.id > 0
      ? String(currentUser.id)
      : null;

  return normalizedUsername || normalizedUserId || null;
};

export function useAudioTranscriptionController(
  currentUser: UserRespDTO | null,
) {
  const [isRecording, setIsRecording] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [transcriptionState, dispatchTranscription] = useReducer(
    reduceAudioTranscriptionState,
    undefined,
    createInitialAudioTranscriptionState,
  );
  const shutdownPromiseRef = useRef<Promise<void> | null>(null);
  const cleanupRef = useRef<() => Promise<void>>(async () => undefined);
  const activeStartTokenRef = useRef<symbol | null>(null);

  const {
    connect: connectTransport,
    disconnect: disconnectTransport,
    stop: stopTransport,
    sendAudioChunk,
  } = useAudioTranscriptionTransport({
    userId: resolveAudioUserId(currentUser),
    onReplace: useCallback((text: string) => {
      dispatchTranscription({
        kind: "replace",
        text,
      });
    }, []),
    onArchive: useCallback((text: string) => {
      dispatchTranscription({
        kind: "archive",
        text,
      });
    }, []),
    onError: useCallback((message: string) => {
      setError(message);
      void cleanupRef.current();
    }, []),
  });

  const stream = useMicrophonePcmStream({
    sampleRate: AUDIO_SAMPLE_RATE,
    onChunk: sendAudioChunk,
    onError: useCallback((streamError: unknown) => {
      console.error("Microphone PCM stream failed:", streamError);
      setError(START_RECORDING_ERROR);
      void cleanupRef.current();
    }, []),
  });

  const { start: startStream, stop: stopStream } = stream;

  const runShutdown = useCallback(
    async (executor: () => Promise<void>) => {
      if (shutdownPromiseRef.current) {
        await shutdownPromiseRef.current;
        return;
      }

      shutdownPromiseRef.current = (async () => {
        activeStartTokenRef.current = null;
        try {
          await executor();
        } finally {
          setIsRecording(false);
        }
      })();

      try {
        await shutdownPromiseRef.current;
      } finally {
        shutdownPromiseRef.current = null;
      }
    },
    [],
  );

  const cleanup = useCallback(async () => {
    await runShutdown(async () => {
      disconnectTransport();
      await stopStream();
    });
  }, [disconnectTransport, runShutdown, stopStream]);

  const finalizeRecording = useCallback(async () => {
    await runShutdown(async () => {
      try {
        await stopStream();
        await stopTransport();
      } catch (stopError) {
        console.error("Finalize recording failed:", stopError);
        disconnectTransport();
      }
    });
  }, [disconnectTransport, runShutdown, stopStream, stopTransport]);

  useEffect(() => {
    cleanupRef.current = cleanup;
  }, [cleanup]);

  const startRecording = useCallback(async () => {
    if (!currentUser) {
      setError("User is not logged in");
      return;
    }

    try {
      if (isRecording) {
        return;
      }
      if (shutdownPromiseRef.current) {
        await shutdownPromiseRef.current;
      }

      const startToken = Symbol("audio-transcription-start");
      activeStartTokenRef.current = startToken;
      setError(null);
      dispatchTranscription({
        kind: "reset",
      });
      connectTransport();
      await startStream();
      if (activeStartTokenRef.current !== startToken) {
        return;
      }
      setIsRecording(true);
    } catch (startError) {
      console.error("Start recording failed:", startError);
      setError(START_RECORDING_ERROR);
      await cleanup();
    }
  }, [
    cleanup,
    connectTransport,
    currentUser,
    isRecording,
    startStream,
  ]);

  const stopRecording = useCallback(() => {
    if (!isRecording && !shutdownPromiseRef.current) {
      return;
    }
    void finalizeRecording();
  }, [finalizeRecording, isRecording]);

  useEffect(() => {
    return () => {
      void cleanup();
    };
  }, [cleanup]);

  return useMemo(
    () => ({
      isRecording,
      currentSentence: transcriptionState.liveText,
      historySentences: transcriptionState.finalText
        ? transcriptionState.finalText
            .split(/\n\n+/)
            .map((sentence) => sentence.trim())
            .filter(Boolean)
        : [],
      transcription: getMergedAudioTranscription(transcriptionState),
      error,
      startRecording,
      stopRecording,
    }),
    [error, isRecording, startRecording, stopRecording, transcriptionState],
  );
}

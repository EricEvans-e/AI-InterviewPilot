import { useCallback, useEffect, useMemo, useRef } from "react";
import { AudioToTextWebSocket } from "@/services/audioToTextWs";

const STOP_TRANSCRIPTION_TIMEOUT_MS = 8000;

type UseAudioTranscriptionTransportParams = {
  userId: string | null;
  onReplace: (text: string) => void;
  onArchive: (text: string) => void;
  onError: (message: string) => void;
};

export function useAudioTranscriptionTransport({
  userId,
  onReplace,
  onArchive,
  onError,
}: UseAudioTranscriptionTransportParams) {
  const transportRef = useRef<AudioToTextWebSocket | null>(null);
  const pendingStopRef = useRef<{
    promise: Promise<void>;
    settle: () => void;
  } | null>(null);
  const onReplaceRef = useRef(onReplace);
  const onArchiveRef = useRef(onArchive);
  const onErrorRef = useRef(onError);

  useEffect(() => {
    onReplaceRef.current = onReplace;
  }, [onReplace]);

  useEffect(() => {
    onArchiveRef.current = onArchive;
  }, [onArchive]);

  useEffect(() => {
    onErrorRef.current = onError;
  }, [onError]);

  const settlePendingStop = useCallback(() => {
    pendingStopRef.current?.settle();
  }, []);

  const createPendingStop = useCallback(() => {
    if (pendingStopRef.current) {
      return pendingStopRef.current.promise;
    }

    let settled = false;
    let resolvePromise: (() => void) | null = null;
    const timeoutId = window.setTimeout(() => {
      settle();
    }, STOP_TRANSCRIPTION_TIMEOUT_MS);
    const promise = new Promise<void>((resolve) => {
      resolvePromise = resolve;
    });
    const settle = () => {
      if (settled) {
        return;
      }
      settled = true;
      window.clearTimeout(timeoutId);
      pendingStopRef.current = null;
      resolvePromise?.();
    };

    pendingStopRef.current = {
      promise,
      settle,
    };
    return promise;
  }, []);

  const disconnect = useCallback(() => {
    settlePendingStop();

    const transport = transportRef.current;
    transportRef.current = null;

    if (!transport) {
      return;
    }

    transport.disconnect();
  }, [settlePendingStop]);

  const stop = useCallback(async () => {
    const transport = transportRef.current;
    if (!transport) {
      return;
    }

    const pendingStop = createPendingStop();

    try {
      transport.sendCommand("stop_transcription");
    } catch (error) {
      console.error("Failed to send stop command", error);
      settlePendingStop();
    }

    await pendingStop;

    if (transportRef.current === transport) {
      transportRef.current = null;
    }
    transport.disconnect();
  }, [createPendingStop, settlePendingStop]);

  const connect = useCallback(() => {
    if (!userId) {
      throw new Error("Audio transcription requires a valid user id");
    }

    disconnect();

    const transport = new AudioToTextWebSocket(userId);
    transport.onSocketOpen = () => {
      transport.sendCommand("start_transcription");
    };
    transport.onTranscription = (text) => {
      onReplaceRef.current(text);
    };
    transport.onFinal = (text) => {
      onArchiveRef.current(text);
      settlePendingStop();
    };
    transport.onError = (message) => {
      settlePendingStop();
      onErrorRef.current(message);
    };
    transport.onDisconnected = () => {
      settlePendingStop();
    };

    transportRef.current = transport;
    transport.connect();
  }, [disconnect, settlePendingStop, userId]);

  const sendAudioChunk = useCallback((data: ArrayBuffer) => {
    transportRef.current?.sendAudio(data);
  }, []);

  useEffect(() => disconnect, [disconnect]);

  return useMemo(
    () => ({
      connect,
      disconnect,
      stop,
      sendAudioChunk,
    }),
    [connect, disconnect, stop, sendAudioChunk],
  );
}

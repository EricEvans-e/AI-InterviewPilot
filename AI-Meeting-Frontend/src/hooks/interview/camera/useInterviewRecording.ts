import { useCallback, useEffect, useRef, useState } from "react";

export type InterviewRecordingHandle = {
  startRecording: () => void;
  stopRecording: () => Promise<Blob | null>;
  isRecording: boolean;
  recordingDuration: number;
};

type UseInterviewRecordingOptions = {
  mimeType?: string;
  enabled?: boolean;
};

export function useInterviewRecording(
  stream: MediaStream | null,
  options: UseInterviewRecordingOptions = {},
): InterviewRecordingHandle {
  const { mimeType: preferredMime, enabled = true } = options;
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingDuration, setRecordingDuration] = useState(0);
  const durationIntervalRef = useRef<ReturnType<typeof setInterval> | null>(
    null,
  );
  const startTimeRef = useRef<number>(0);

  const resolveMimeType = useCallback((): string | undefined => {
    if (typeof MediaRecorder === "undefined") return undefined;
    if (preferredMime && MediaRecorder.isTypeSupported(preferredMime)) {
      return preferredMime;
    }
    const candidates = [
      "video/webm;codecs=vp9,opus",
      "video/webm;codecs=vp8,opus",
      "video/webm",
      "video/mp4",
    ];
    return candidates.find((type) => MediaRecorder.isTypeSupported(type));
  }, [preferredMime]);

  const startRecording = useCallback(() => {
    if (!enabled || !stream || isRecording) return;

    const mimeType = resolveMimeType();
    if (!mimeType) {
      console.warn("[useInterviewRecording] No supported MIME type found");
      return;
    }

    chunksRef.current = [];
    const recorder = new MediaRecorder(stream, {
      mimeType,
      videoBitsPerSecond: 2500000,
    });

    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        chunksRef.current.push(event.data);
      }
    };

    recorder.onerror = (event) => {
      console.error("[useInterviewRecording] MediaRecorder error:", event);
      setIsRecording(false);
      if (durationIntervalRef.current) {
        clearInterval(durationIntervalRef.current);
        durationIntervalRef.current = null;
      }
    };

    mediaRecorderRef.current = recorder;
    recorder.start(1000);
    setIsRecording(true);
    setRecordingDuration(0);
    startTimeRef.current = Date.now();

    durationIntervalRef.current = setInterval(() => {
      setRecordingDuration(
        Math.floor((Date.now() - startTimeRef.current) / 1000),
      );
    }, 1000);
  }, [enabled, stream, isRecording, resolveMimeType]);

  const stopRecording = useCallback((): Promise<Blob | null> => {
    return new Promise((resolve) => {
      const recorder = mediaRecorderRef.current;
      if (!recorder || recorder.state === "inactive") {
        setIsRecording(false);
        if (durationIntervalRef.current) {
          clearInterval(durationIntervalRef.current);
          durationIntervalRef.current = null;
        }
        resolve(null);
        return;
      }

      recorder.onstop = () => {
        // Small delay to ensure all data chunks are flushed to the blob
        setTimeout(() => {
          const mimeType = recorder.mimeType || "video/webm";
          const blob = new Blob(chunksRef.current, { type: mimeType });
          chunksRef.current = [];
          setIsRecording(false);
          if (durationIntervalRef.current) {
            clearInterval(durationIntervalRef.current);
            durationIntervalRef.current = null;
          }
          resolve(blob.size > 0 ? blob : null);
        }, 200);
      };

      // Request any remaining buffered data before stopping
      if (recorder.state === "recording") {
        recorder.requestData();
      }
      recorder.stop();
    });
  }, []);

  useEffect(() => {
    return () => {
      if (durationIntervalRef.current) {
        clearInterval(durationIntervalRef.current);
        durationIntervalRef.current = null;
      }
      const recorder = mediaRecorderRef.current;
      if (recorder && recorder.state !== "inactive") {
        recorder.stop();
      }
      mediaRecorderRef.current = null;
      chunksRef.current = [];
    };
  }, []);

  return { startRecording, stopRecording, isRecording, recordingDuration };
}

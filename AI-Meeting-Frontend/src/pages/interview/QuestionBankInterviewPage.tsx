import { startTransition, useCallback, useEffect, useRef, useState } from "react";
import type { CameraPreviewHandle } from "@/components/camera/CameraPreview";
import ChatRoom from "@/components/chat/ChatRoom";
import SmartComposer from "@/components/chat/SmartComposer";
import { DigitalHumanAvatar } from "@/components/interview/DigitalHumanAvatar";
import InterviewCameraOverlay from "@/components/interview/InterviewCameraOverlay";
import InterviewHeader from "@/components/interview/InterviewHeader";
import InterviewSketchpadSheet from "@/components/interview/sketchpad/InterviewSketchpadSheet";
import { useInterviewDemeanorPolling } from "@/hooks/interview/camera/useInterviewDemeanorPolling";
import { useInterviewRecording } from "@/hooks/interview/camera/useInterviewRecording";
import { useQuestionBankPageController } from "@/hooks/interview/useQuestionBankPageController";

/**
 * Interview page for question-bank mode.
 * No resume upload/preview - questions come directly from the question bank.
 */
export default function QuestionBankInterviewPage() {
  const cameraPreviewRef = useRef<CameraPreviewHandle | null>(null);
  const [isSketchpadOpen, setIsSketchpadOpen] = useState(false);
  const [isQuestionTtsPlaying, setIsQuestionTtsPlaying] = useState(false);
  const { chat, interview, camera } = useQuestionBankPageController();
  const { setInput, isReady, isSubmitting, handleSend, input, messages } = chat;

  const [cameraStream, setCameraStream] = useState<MediaStream | null>(null);
  const recording = useInterviewRecording(cameraStream, {
    enabled: isReady && camera.isOpen,
  });
  const {
    isRecording,
    startRecording,
    stopRecording,
  } = recording;
  const { handleEndInterview } = interview;

  const captureFrame = useCallback(async () => {
    return cameraPreviewRef.current?.captureFrame() ?? null;
  }, []);

  const handleCameraStreamChange = useCallback((stream: MediaStream | null) => {
    setCameraStream(stream);
  }, []);

  // Auto-start recording when interview is ready and camera stream is available
  const hasStartedRecordingRef = useRef(false);
  useEffect(() => {
    if (isReady && camera.isOpen && cameraStream && !isRecording && !hasStartedRecordingRef.current) {
      hasStartedRecordingRef.current = true;
      startRecording();
    }
    if (!isReady || !camera.isOpen) {
      hasStartedRecordingRef.current = false;
    }
  }, [isReady, camera.isOpen, cameraStream, isRecording, startRecording]);

  // Wrap end interview to stop recording first
  const handleEndInterviewWithRecording = useCallback(async () => {
    let recordingBlob: Blob | null = null;
    if (isRecording) {
      recordingBlob = await stopRecording();
    }
    await handleEndInterview({ recordingBlob });
  }, [handleEndInterview, isRecording, stopRecording]);

  const handleInsertNotes = useCallback(
    (notes: string) => {
      if (!notes.trim()) return;
      setInput((prev) => (prev.trim() ? `${prev.trim()}\n\n${notes}` : notes));
      setIsSketchpadOpen(false);
    },
    [setInput],
  );

  const handleOpenSketchpad = useCallback(() => {
    startTransition(() => {
      setIsSketchpadOpen(true);
    });
  }, []);

  useInterviewDemeanorPolling({
    sessionId: interview.sessionId,
    enabled:
      Boolean(interview.sessionId) &&
      isReady &&
      camera.isOpen &&
      !interview.isFinished &&
      !interview.isEnding,
    captureFrame,
  });

  return (
    <>
      <ChatRoom
        header={
          <InterviewHeader
            isReady={isReady}
            currentQuestionNumber={interview.currentQuestionNumber}
            currentQuestionContent={interview.currentQuestionContent}
            isCurrentQuestionFollowUp={interview.isCurrentQuestionFollowUp}
            currentFollowUpCount={interview.currentFollowUpCount}
            isInterviewFinished={interview.isFinished}
            totalInterviewScore={interview.totalScore}
            isCameraOpen={camera.isOpen}
            isEndingInterview={interview.isEnding}
            onToggleCamera={camera.handleToggleCamera}
            onOpenSketchpad={handleOpenSketchpad}
            onEndInterview={handleEndInterviewWithRecording}
          />
        }
        topContent={
          interview.error ? (
            <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
              {interview.error}
            </div>
          ) : null
        }
        messages={messages}
        inputValue={input}
        onInputChange={setInput}
        onSend={handleSend}
        customComposer={
          <SmartComposer
            value={input}
            onChange={setInput}
            onSend={handleSend}
            placeholder="输入你的回答，或点击麦克风开始语音作答..."
            disabled={!isReady || isSubmitting}
            showDefaultLeading={false}
          />
        }
        onTtsPlaybackStateChange={setIsQuestionTtsPlaying}
        contentOverlay={
          <>
            <InterviewCameraOverlay
              ref={cameraPreviewRef}
              isCameraOpen={camera.isOpen}
              isCameraExpanded={camera.isExpanded}
              cameraErrorCopy={camera.errorCopy}
              onCameraError={camera.handleCameraError}
              onToggleExpanded={camera.handleToggleExpanded}
              onStreamChange={handleCameraStreamChange}
              isRecording={isRecording}
            />
            <DigitalHumanAvatar
              isSpeaking={isQuestionTtsPlaying}
              isListening={isReady && !isSubmitting}
              isThinking={isSubmitting}
              text={interview.currentQuestionContent ?? undefined}
            />
          </>
        }
        footer={
          <div className="mt-2 space-y-2">
            <p className="text-center text-xs text-slate-400">
              AI 生成内容可能存在误差，请以实际情况为准。
            </p>
          </div>
        }
      />

      <InterviewSketchpadSheet
        open={isSketchpadOpen}
        onOpenChange={setIsSketchpadOpen}
        sessionId={interview.sessionId}
        currentQuestionNumber={interview.currentQuestionNumber}
        currentQuestionContent={interview.currentQuestionContent}
        onInsertNotes={handleInsertNotes}
      />
    </>
  );
}

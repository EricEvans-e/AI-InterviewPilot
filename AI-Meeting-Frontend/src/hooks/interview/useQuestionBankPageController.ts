import { useInterviewCameraState } from "@/hooks/interview/camera/useInterviewCameraState";
import { useInterviewSessionFlow } from "@/hooks/interview/session/useInterviewSessionFlow";
import { useAppSelector } from "@/store/hooks";

/**
 * Controller hook for question-bank interview mode.
 * Excludes resume analysis since question-bank sessions don't use resumes.
 */
export function useQuestionBankPageController() {
  const { currentUser } = useAppSelector((state) => state.user);

  const sessionFlow = useInterviewSessionFlow(currentUser);
  const cameraState = useInterviewCameraState();

  return {
    chat: {
      messages: sessionFlow.messages,
      input: sessionFlow.input,
      setInput: sessionFlow.setInput,
      isReady: sessionFlow.isReady,
      isSubmitting: sessionFlow.isInterviewSubmitting,
      handleSend: sessionFlow.handleSend,
    },
    interview: {
      sessionId: sessionFlow.interviewerSessionId,
      error: sessionFlow.interviewError,
      isEnding: sessionFlow.isEndingInterview,
      currentQuestionNumber: sessionFlow.currentQuestionNumber,
      currentQuestionContent: sessionFlow.currentQuestionContent,
      isCurrentQuestionFollowUp: sessionFlow.isCurrentQuestionFollowUp,
      currentFollowUpCount: sessionFlow.currentFollowUpCount,
      isFinished: sessionFlow.isInterviewFinished,
      totalScore: sessionFlow.totalInterviewScore,
      handleEndInterview: sessionFlow.handleEndInterview,
    },
    camera: {
      isOpen: cameraState.isCameraOpen,
      isExpanded: cameraState.isCameraExpanded,
      errorCopy: cameraState.cameraErrorCopy,
      handleCameraError: cameraState.handleCameraError,
      handleToggleCamera: cameraState.handleToggleCamera,
      handleToggleExpanded: cameraState.handleToggleCameraExpanded,
    },
  };
}

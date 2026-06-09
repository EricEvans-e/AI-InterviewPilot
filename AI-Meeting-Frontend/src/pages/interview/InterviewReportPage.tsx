import { useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { Card } from "@/components/ui/card";
import InterviewReportHeader from "@/components/interview/report/InterviewReportHeader";
import InterviewRecordingPlaybackCard from "@/components/interview/report/InterviewRecordingPlaybackCard";
import InterviewScoreAndRadarCard from "@/components/interview/report/InterviewScoreAndRadarCard";
import InterviewConclusionCard from "@/components/interview/report/InterviewConclusionCard";
import InterviewNextActionsCard from "@/components/interview/report/InterviewNextActionsCard";
import InterviewQaReplayCard from "@/components/interview/report/InterviewQaReplayCard";
import InterviewEtiquetteCard from "@/components/interview/report/InterviewEtiquetteCard";
import InterviewReferenceAnswerCard from "@/components/interview/report/InterviewReferenceAnswerCard";
import InterviewTeacherReviewCard from "@/components/interview/report/InterviewTeacherReviewCard";
import { useInterviewReportData } from "@/hooks/interview/report/useInterviewReportData";
import { getReportSessionIdFromLocation } from "@/lib/interviewReportRoute";

export default function InterviewReportPage() {
  const location = useLocation();
  const reportSessionId = getReportSessionIdFromLocation(location);

  const {
    isRecordLoading,
    recordError,
    record,
    isGeneratingAiReviewFeedback,
    canGenerateAiReviewFeedback,
    generateAiReviewFeedback,
    isGeneratingReferenceAnswers,
    canGenerateReferenceAnswers,
    generateReferenceAnswers,
    resumeScore,
    interviewScore,
    compositeScore,
    isCompositeEstimated,
    radarPoints,
    dimensionScores,
    sortedSuggestions,
    interviewDirection,
    qaReviews,
    reviewFeedback,
    recordingUrl,
  } = useInterviewReportData(reportSessionId);

  return (
    <div className="h-full overflow-y-auto bg-white">
      <div className="max-w-7xl mx-auto px-6 py-10 space-y-8">
        <motion.div
          initial={{ opacity: 0, y: 16, filter: "blur(6px)" }}
          animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
          transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
        >
          <InterviewReportHeader />
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 16, filter: "blur(6px)" }}
          animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
          transition={{ duration: 0.35, delay: 0.02, ease: [0.22, 1, 0.36, 1] }}
        >
          <InterviewRecordingPlaybackCard
            recordingUrl={recordingUrl}
            isLoading={isRecordLoading}
          />
        </motion.div>

        {!reportSessionId && (
          <Card className="p-4 border-amber-100 bg-amber-50 text-amber-700 text-sm">
            未获取到会话 ID，当前显示为空报告。请从面试流程结束后进入本页。
          </Card>
        )}

        <div className="grid items-start gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <div className="min-w-0 space-y-6">
            <motion.div
              className="min-w-0"
              initial={{ opacity: 0, y: 18, filter: "blur(5px)" }}
              animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              transition={{
                duration: 0.38,
                delay: 0.05,
                ease: [0.22, 1, 0.36, 1],
              }}
            >
              <InterviewScoreAndRadarCard
                resumeScore={resumeScore}
                interviewScore={interviewScore}
                compositeScore={compositeScore}
                isCompositeEstimated={isCompositeEstimated}
                radarPoints={radarPoints}
                dimensionScores={dimensionScores}
              />
            </motion.div>
            <motion.div
              className="min-w-0"
              initial={{ opacity: 0, y: 18, filter: "blur(5px)" }}
              animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
              transition={{
                duration: 0.38,
                delay: 0.12,
                ease: [0.22, 1, 0.36, 1],
              }}
            >
              <InterviewNextActionsCard
                reviewFeedback={reviewFeedback}
                sortedSuggestions={sortedSuggestions}
                isRecordLoading={isRecordLoading}
                recordError={recordError}
              />
            </motion.div>
          </div>
          <motion.div
            className="min-w-0"
            initial={{ opacity: 0, y: 18, filter: "blur(5px)" }}
            animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            transition={{
              duration: 0.38,
              delay: 0.18,
              ease: [0.22, 1, 0.36, 1],
            }}
          >
            <InterviewConclusionCard
              interviewDirection={interviewDirection}
              reviewFeedback={reviewFeedback}
              isRecordLoading={isRecordLoading}
              recordError={recordError}
              canGenerate={canGenerateAiReviewFeedback}
              isGenerating={isGeneratingAiReviewFeedback}
              onGenerate={generateAiReviewFeedback}
            />
          </motion.div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 20, filter: "blur(5px)" }}
          animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
          transition={{ duration: 0.4, delay: 0.24, ease: [0.22, 1, 0.36, 1] }}
        >
          <InterviewQaReplayCard
            qaReviews={qaReviews}
            isRecordLoading={isRecordLoading}
            recordError={recordError}
          />
        </motion.div>

        <div className="grid items-start gap-6 lg:grid-cols-[minmax(320px,0.45fr)_minmax(0,1fr)]">
          <motion.div
            className="min-w-0"
            initial={{ opacity: 0, y: 20, filter: "blur(5px)" }}
            animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            transition={{ duration: 0.4, delay: 0.3, ease: [0.22, 1, 0.36, 1] }}
          >
            <InterviewEtiquetteCard
              record={record}
              isLoading={isRecordLoading}
            />
          </motion.div>
          <motion.div
            className="min-w-0"
            initial={{ opacity: 0, y: 20, filter: "blur(5px)" }}
            animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            transition={{
              duration: 0.4,
              delay: 0.36,
              ease: [0.22, 1, 0.36, 1],
            }}
          >
            <InterviewReferenceAnswerCard
              qaReviews={qaReviews}
              isLoading={isRecordLoading}
              canGenerate={canGenerateReferenceAnswers}
              isGenerating={isGeneratingReferenceAnswers}
              onGenerate={generateReferenceAnswers}
            />
          </motion.div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 20, filter: "blur(5px)" }}
          animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
          transition={{ duration: 0.4, delay: 0.42, ease: [0.22, 1, 0.36, 1] }}
        >
          <InterviewTeacherReviewCard sessionId={reportSessionId} />
        </motion.div>
      </div>
    </div>
  );
}

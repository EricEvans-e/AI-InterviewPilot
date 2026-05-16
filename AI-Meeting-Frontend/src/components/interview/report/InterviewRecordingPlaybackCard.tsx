import { cn } from "@/lib/utils";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

type InterviewRecordingPlaybackCardProps = {
  recordingUrl: string | null;
  isLoading: boolean;
};

export default function InterviewRecordingPlaybackCard({
  recordingUrl,
  isLoading,
}: InterviewRecordingPlaybackCardProps) {
  return (
    <Card className="overflow-hidden">
      <CardHeader className="pb-3">
        <CardTitle className="text-base font-medium text-slate-800">
          面试录像回放
        </CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex h-48 items-center justify-center rounded-lg bg-slate-50">
            <div className="h-6 w-6 animate-spin rounded-full border-2 border-slate-300 border-t-slate-600" />
          </div>
        ) : recordingUrl ? (
          <video
            controls
            playsInline
            src={recordingUrl}
            className={cn(
              "w-full max-w-2xl rounded-lg bg-black",
              "aspect-video",
            )}
          >
            Your browser does not support video playback.
          </video>
        ) : (
          <div className="flex h-48 flex-col items-center justify-center gap-2 rounded-lg bg-slate-50 text-slate-400">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="32"
              height="32"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="m16 13 5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5" />
              <rect x="2" y="6" width="14" height="12" rx="2" />
            </svg>
            <span className="text-sm">暂无录像</span>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

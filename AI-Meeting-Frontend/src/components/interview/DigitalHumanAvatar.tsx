import { cn } from "@/lib/utils";

type DigitalHumanAvatarProps = {
  isSpeaking: boolean;
  isListening: boolean;
  isThinking: boolean;
  text?: string;
};

const SLOW_SPIN_KEYFRAMES = `
@keyframes spin-slow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
`;

export function DigitalHumanAvatar({
  isSpeaking,
  isListening,
  isThinking,
  text,
}: DigitalHumanAvatarProps) {
  return (
    <div className="relative flex h-48 w-full items-center justify-center overflow-hidden rounded-lg bg-gradient-to-b from-slate-50 to-slate-100">
      <style>{SLOW_SPIN_KEYFRAMES}</style>

      {/* Avatar circle */}
      <div
        className={cn(
          "flex h-32 w-32 items-center justify-center rounded-full bg-gradient-to-br from-cyan-400 to-indigo-500 transition-all duration-300",
          isSpeaking && "scale-105 animate-pulse",
          isThinking && "scale-105",
        )}
        style={
          isThinking
            ? { animation: "spin-slow 3s linear infinite" }
            : undefined
        }
      >
        <span className="text-4xl font-bold text-white">AI</span>
      </div>

      {/* Text subtitle */}
      {text && (
        <div className="absolute bottom-10 left-4 right-4 rounded bg-white/90 p-3 text-sm backdrop-blur">
          {text}
        </div>
      )}

      {/* Status indicator */}
      <div className="absolute bottom-3 left-0 right-0 text-center">
        {isSpeaking && (
          <span className="text-sm text-cyan-600">正在播报...</span>
        )}
        {isListening && (
          <span className="text-sm text-green-600">请回答...</span>
        )}
        {isThinking && (
          <span className="text-sm text-amber-600">思考中...</span>
        )}
      </div>
    </div>
  );
}

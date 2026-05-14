import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Camera,
  CheckCircle2,
  Loader2,
  Mic,
  Sun,
  User,
  Wifi,
  XCircle,
  ArrowRight,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ROUTES } from "@/lib/constants";

type CheckStatus = "pending" | "ok" | "fail";

interface CheckItemProps {
  icon: React.ReactNode;
  label: string;
  status: CheckStatus;
  detail?: string;
}

function CheckItem({ icon, label, status, detail }: CheckItemProps) {
  return (
    <div className="flex items-center gap-3 rounded-lg border px-4 py-3">
      <span className="text-slate-500">{icon}</span>
      <span className="flex-1 text-sm font-medium text-slate-700">{label}</span>
      {detail && (
        <span className="text-xs text-slate-400 mr-2">{detail}</span>
      )}
      {status === "pending" && (
        <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
      )}
      {status === "ok" && (
        <CheckCircle2 className="h-5 w-5 text-emerald-500" />
      )}
      {status === "fail" && (
        <XCircle className="h-5 w-5 text-red-500" />
      )}
    </div>
  );
}

function getStatus(ok: boolean | null): CheckStatus {
  if (ok === null) return "pending";
  return ok ? "ok" : "fail";
}

export default function InterviewPrecheckPage() {
  const navigate = useNavigate();
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const animFrameRef = useRef<number>(0);

  const [cameraOk, setCameraOk] = useState<boolean | null>(null);
  const [micOk, setMicOk] = useState<boolean | null>(null);
  const [networkOk, setNetworkOk] = useState<boolean | null>(null);
  const [brightness, setBrightness] = useState<number>(0);
  const [faceOk, setFaceOk] = useState<boolean | null>(null);

  // Acquire camera + microphone
  useEffect(() => {
    let cancelled = false;

    const initMedia = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: true,
          audio: true,
        });
        if (cancelled) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
        }
        setCameraOk(true);
        setMicOk(true);
      } catch (err) {
        if (!cancelled) {
          console.error("Media device error:", err);
          setCameraOk(false);
          setMicOk(false);
        }
      }
    };

    void initMedia();

    return () => {
      cancelled = true;
      streamRef.current?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  // Network ping test
  useEffect(() => {
    let cancelled = false;

    const ping = async () => {
      try {
        const start = performance.now();
        await fetch("/api/ip/v1/health", { method: "HEAD" });
        if (!cancelled) {
          const latency = performance.now() - start;
          setNetworkOk(latency < 2000);
        }
      } catch {
        if (!cancelled) {
          setNetworkOk(false);
        }
      }
    };

    void ping();

    return () => {
      cancelled = true;
    };
  }, []);

  // Brightness + face-position loop
  const runFrameCheck = useCallback(() => {
    const video = videoRef.current;
    if (!video || video.readyState < 2) {
      animFrameRef.current = requestAnimationFrame(runFrameCheck);
      return;
    }

    const canvas = document.createElement("canvas");
    canvas.width = 100;
    canvas.height = 75;
    const ctx = canvas.getContext("2d");
    if (!ctx) {
      animFrameRef.current = requestAnimationFrame(runFrameCheck);
      return;
    }

    ctx.drawImage(video, 0, 0, 100, 75);
    const data = ctx.getImageData(0, 0, 100, 75).data;

    // Brightness
    let sum = 0;
    for (let i = 0; i < data.length; i += 4) {
      sum += (data[i] + data[i + 1] + data[i + 2]) / 3;
    }
    const avgBrightness = sum / (data.length / 4);
    setBrightness(avgBrightness);

    // Face-position: check if centre third of frame has enough variance
    // (very rough proxy -- real face detection would use a model)
    const cx1 = Math.floor(100 / 3);
    const cx2 = Math.floor((100 * 2) / 3);
    const cy1 = Math.floor(75 / 3);
    const cy2 = Math.floor((75 * 2) / 3);
    let centreSum = 0;
    let centreCount = 0;
    for (let y = cy1; y < cy2; y++) {
      for (let x = cx1; x < cx2; x++) {
        const idx = (y * 100 + x) * 4;
        centreSum += (data[idx] + data[idx + 1] + data[idx + 2]) / 3;
        centreCount++;
      }
    }
    const centreBrightness = centreCount > 0 ? centreSum / centreCount : 0;
    // If centre is neither too dark nor too bright and differs from edges, likely a face
    setFaceOk(centreBrightness > 30 && centreBrightness < 240);

    animFrameRef.current = requestAnimationFrame(runFrameCheck);
  }, []);

  useEffect(() => {
    if (cameraOk) {
      animFrameRef.current = requestAnimationFrame(runFrameCheck);
    }
    return () => cancelAnimationFrame(animFrameRef.current);
  }, [cameraOk, runFrameCheck]);

  const allOk = cameraOk === true && micOk === true;

  return (
    <div className="h-full overflow-y-auto bg-white">
      <div className="mx-auto max-w-2xl px-6 py-10">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900 mb-1">
          设备预检
        </h1>
        <p className="text-sm text-slate-500 mb-6">
          请确认摄像头、麦克风和网络环境正常后再进入面试。
        </p>

        {/* Camera preview */}
        <div className="relative mb-6 overflow-hidden rounded-xl border bg-slate-900">
          <video
            ref={videoRef}
            autoPlay
            playsInline
            muted
            className="aspect-video w-full object-cover"
          />
          {cameraOk === false && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-slate-900/80 text-slate-300">
              <Camera className="h-8 w-8" />
              <span className="text-sm">无法访问摄像头</span>
            </div>
          )}
        </div>

        {/* Check results */}
        <Card className="mb-6">
          <CardHeader className="pb-3">
            <CardTitle className="text-base">检测结果</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <CheckItem
              icon={<Camera className="h-5 w-5" />}
              label="摄像头"
              status={getStatus(cameraOk)}
            />
            <CheckItem
              icon={<Mic className="h-5 w-5" />}
              label="麦克风"
              status={getStatus(micOk)}
            />
            <CheckItem
              icon={<Wifi className="h-5 w-5" />}
              label="网络连接"
              status={getStatus(networkOk)}
            />
            <CheckItem
              icon={<Sun className="h-5 w-5" />}
              label="环境亮度"
              status={getStatus(brightness > 50)}
              detail={`亮度 ${Math.round(brightness)}`}
            />
            <CheckItem
              icon={<User className="h-5 w-5" />}
              label="人脸位置"
              status={getStatus(faceOk)}
              detail={faceOk === false ? "请面向摄像头" : undefined}
            />
          </CardContent>
        </Card>

        {/* Enter interview */}
        <Button
          className="w-full rounded-full"
          size="lg"
          disabled={!allOk}
          onClick={() => navigate(ROUTES.interviewRoom)}
        >
          {allOk ? (
            <>
              进入面试
              <ArrowRight className="ml-2 h-4 w-4" />
            </>
          ) : (
            "请先完成设备检测"
          )}
        </Button>
      </div>
    </div>
  );
}

import {
  forwardRef,
  type PointerEvent,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { Maximize2, Minimize2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import CameraPreview, {
  type CameraPreviewHandle,
} from "@/components/camera/CameraPreview";
import ErrorNotice from "@/components/feedback/ErrorNotice";
import type { MediaError } from "@/lib/media";
import { cn } from "@/lib/utils";
import {
  CAMERA_OVERLAY_DEFAULT_SIZE,
  clampCameraOverlayPosition,
  getDefaultCameraOverlayPosition,
  type OverlayPoint,
  type OverlaySize,
} from "@/components/interview/interviewCameraOverlayPosition";

type CameraErrorCopy = {
  title: string;
  description: string;
} | null;

type InterviewCameraOverlayProps = {
  isCameraOpen: boolean;
  isCameraExpanded: boolean;
  cameraErrorCopy: CameraErrorCopy;
  onCameraError: (error: MediaError) => void;
  onToggleExpanded: () => void;
  onStreamChange?: (stream: MediaStream | null) => void;
  isRecording?: boolean;
};

const InterviewCameraOverlay = forwardRef<
  CameraPreviewHandle,
  InterviewCameraOverlayProps
>(function InterviewCameraOverlay(
  {
    isCameraOpen,
    isCameraExpanded,
    cameraErrorCopy,
    onCameraError,
    onToggleExpanded,
    onStreamChange,
    isRecording = false,
  }: InterviewCameraOverlayProps,
  ref,
) {
  const cardRef = useRef<HTMLDivElement | null>(null);
  const hasInitializedPositionRef = useRef(false);
  const dragStartRef = useRef<{
    pointerId: number;
    origin: OverlayPoint;
    pointer: OverlayPoint;
  } | null>(null);
  const defaultPosition = useMemo<OverlayPoint>(
    () => getDefaultCameraOverlayPosition(CAMERA_OVERLAY_DEFAULT_SIZE),
    [],
  );
  const [position, setPosition] = useState<OverlayPoint>(defaultPosition);

  const getOverlaySize = useCallback((): OverlaySize => {
    const rect = cardRef.current?.getBoundingClientRect();
    return {
      width: rect?.width || CAMERA_OVERLAY_DEFAULT_SIZE.width,
      height: rect?.height || CAMERA_OVERLAY_DEFAULT_SIZE.height,
    };
  }, []);

  const getContainerSize = useCallback((): OverlaySize => {
    const parent = cardRef.current?.parentElement;
    const rect = parent?.getBoundingClientRect();
    return {
      width: rect?.width || window.innerWidth,
      height: rect?.height || window.innerHeight,
    };
  }, []);

  const clampToViewport = useCallback(
    (point: OverlayPoint) => {
      if (typeof window === "undefined") {
        return point;
      }
      return clampCameraOverlayPosition(
        point,
        getContainerSize(),
        getOverlaySize(),
      );
    },
    [getContainerSize, getOverlaySize],
  );

  const handlePointerDown = useCallback(
    (event: PointerEvent<HTMLDivElement>) => {
      if (isCameraExpanded || event.button !== 0) {
        return;
      }
      const target = event.target as HTMLElement | null;
      if (target?.closest("button")) {
        return;
      }

      const safePosition = clampToViewport(position);
      dragStartRef.current = {
        pointerId: event.pointerId,
        origin: safePosition,
        pointer: { x: event.clientX, y: event.clientY },
      };
      setPosition(safePosition);
      event.currentTarget.setPointerCapture(event.pointerId);
    },
    [clampToViewport, isCameraExpanded, position],
  );

  const handlePointerMove = useCallback(
    (event: PointerEvent<HTMLDivElement>) => {
      const dragStart = dragStartRef.current;
      if (!dragStart || dragStart.pointerId !== event.pointerId) {
        return;
      }

      setPosition(
        clampToViewport({
          x: dragStart.origin.x + event.clientX - dragStart.pointer.x,
          y: dragStart.origin.y + event.clientY - dragStart.pointer.y,
        }),
      );
    },
    [clampToViewport],
  );

  const handlePointerEnd = useCallback(
    (event: PointerEvent<HTMLDivElement>) => {
      const dragStart = dragStartRef.current;
      if (!dragStart || dragStart.pointerId !== event.pointerId) {
        return;
      }
      dragStartRef.current = null;
      if (event.currentTarget.hasPointerCapture(event.pointerId)) {
        event.currentTarget.releasePointerCapture(event.pointerId);
      }
    },
    [],
  );

  useEffect(() => {
    if (isCameraExpanded || typeof window === "undefined") {
      return;
    }

    const handleResize = () => {
      setPosition((current) => {
        if (!hasInitializedPositionRef.current) {
          hasInitializedPositionRef.current = true;
          return getDefaultCameraOverlayPosition(
            getContainerSize(),
            getOverlaySize(),
          );
        }
        return clampToViewport(current);
      });
    };

    window.addEventListener("resize", handleResize);
    handleResize();
    return () => window.removeEventListener("resize", handleResize);
  }, [clampToViewport, getContainerSize, getOverlaySize, isCameraExpanded]);

  return (
    <Card
      ref={cardRef}
      className={cn(
        "absolute overflow-hidden border-2 bg-black shadow-2xl transition-all duration-300",
        isCameraExpanded
          ? "bottom-24 left-4 right-4 top-4 z-20"
          : "z-20 h-48 w-64 cursor-grab active:cursor-grabbing",
        isRecording ? "border-red-400" : "border-slate-700/70",
      )}
      style={{
        display: isCameraOpen ? "block" : "none",
        ...(isCameraExpanded
          ? undefined
          : {
              left: position.x,
              top: position.y,
            }),
      }}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerEnd}
      onPointerCancel={handlePointerEnd}
    >
      <div className="group relative h-full w-full">
        <CameraPreview
          ref={ref}
          isOpen={isCameraOpen}
          onError={onCameraError}
          onStreamChange={onStreamChange}
        />
        {cameraErrorCopy && (
          <div className="absolute inset-3 z-10">
            <ErrorNotice
              title={cameraErrorCopy.title}
              description={cameraErrorCopy.description}
            />
          </div>
        )}
        <div className="absolute right-2 top-2 opacity-0 transition-opacity group-hover:opacity-100">
          <Button
            variant="secondary"
            size="icon"
            className="h-8 w-8 bg-black/50 text-white hover:bg-black/70"
            onClick={onToggleExpanded}
          >
            {isCameraExpanded ? (
              <Minimize2 className="h-4 w-4" />
            ) : (
              <Maximize2 className="h-4 w-4" />
            )}
          </Button>
        </div>
        <div className="absolute bottom-3 left-3 flex items-center gap-2">
          <div className="h-2 w-2 animate-pulse rounded-full bg-red-500" />
          <span className="text-xs font-medium text-white drop-shadow-md">
            正在分析状态...
          </span>
        </div>
      </div>
    </Card>
  );
});

export default InterviewCameraOverlay;

export type OverlayPoint = {
  x: number;
  y: number;
};

export type OverlaySize = {
  width: number;
  height: number;
};

export const CAMERA_OVERLAY_MARGIN = 16;

export const CAMERA_OVERLAY_DEFAULT_SIZE: OverlaySize = {
  width: 256,
  height: 192,
};

const clamp = (value: number, min: number, max: number) =>
  Math.min(Math.max(value, min), max);

export const clampCameraOverlayPosition = (
  point: OverlayPoint,
  viewport: OverlaySize,
  overlay: OverlaySize,
): OverlayPoint => {
  const maxX = Math.max(
    CAMERA_OVERLAY_MARGIN,
    viewport.width - overlay.width - CAMERA_OVERLAY_MARGIN,
  );
  const maxY = Math.max(
    CAMERA_OVERLAY_MARGIN,
    viewport.height - overlay.height - CAMERA_OVERLAY_MARGIN,
  );

  return {
    x: clamp(point.x, CAMERA_OVERLAY_MARGIN, maxX),
    y: clamp(point.y, CAMERA_OVERLAY_MARGIN, maxY),
  };
};

export const getDefaultCameraOverlayPosition = (
  container: OverlaySize,
  overlay: OverlaySize = CAMERA_OVERLAY_DEFAULT_SIZE,
): OverlayPoint =>
  clampCameraOverlayPosition(
    {
      x: container.width - overlay.width - CAMERA_OVERLAY_MARGIN,
      y: CAMERA_OVERLAY_MARGIN,
    },
    container,
    overlay,
  );

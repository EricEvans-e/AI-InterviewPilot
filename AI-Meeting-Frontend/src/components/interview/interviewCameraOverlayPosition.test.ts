import { describe, expect, it } from "vitest";
import {
  CAMERA_OVERLAY_DEFAULT_SIZE,
  getDefaultCameraOverlayPosition,
  clampCameraOverlayPosition,
} from "@/components/interview/interviewCameraOverlayPosition";

describe("clampCameraOverlayPosition", () => {
  it("keeps the camera overlay inside the viewport", () => {
    expect(
      clampCameraOverlayPosition(
        { x: 1_000, y: -30 },
        { width: 1_200, height: 800 },
        { width: 320, height: 200 },
      ),
    ).toEqual({ x: 864, y: 16 });
  });

  it("uses the lower bound when dragged past the left edge", () => {
    expect(
      clampCameraOverlayPosition(
        { x: -80, y: 240 },
        { width: 900, height: 600 },
        { width: 300, height: 180 },
      ),
    ).toEqual({ x: 16, y: 240 });
  });

  it("places the default compact overlay at the container's top-right corner", () => {
    expect(
      getDefaultCameraOverlayPosition(
        { width: 900, height: 700 },
        CAMERA_OVERLAY_DEFAULT_SIZE,
      ),
    ).toEqual({ x: 628, y: 16 });
  });
});

import type { ComponentProps } from "react";
import { act, render, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import InterviewCameraOverlay from "@/components/interview/InterviewCameraOverlay";

const createMockStream = () =>
  ({
    getTracks: () => [{ stop: vi.fn() }],
  }) as unknown as MediaStream;

const mockGetUserMedia = (stream: MediaStream) => {
  Object.defineProperty(navigator, "mediaDevices", {
    configurable: true,
    value: {
      getUserMedia: vi.fn().mockResolvedValue(stream),
    },
  });
  vi.spyOn(HTMLMediaElement.prototype, "play").mockImplementation(() =>
    Promise.resolve(),
  );
};

const renderOverlay = (
  props: Partial<ComponentProps<typeof InterviewCameraOverlay>> = {},
) =>
  render(
    <div
      data-testid="camera-overlay-container"
      style={{ height: 700, position: "relative", width: 900 }}
    >
      <InterviewCameraOverlay
        isCameraOpen
        isCameraExpanded={false}
        cameraErrorCopy={null}
        onCameraError={vi.fn()}
        onToggleExpanded={vi.fn()}
        {...props}
      />
    </div>,
  );

describe("InterviewCameraOverlay", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("positions the compact camera inside its parent overlay container", async () => {
    const { container, getByTestId } = renderOverlay();
    const parent = getByTestId("camera-overlay-container");
    vi.spyOn(parent, "getBoundingClientRect").mockReturnValue({
      bottom: 700,
      height: 700,
      left: 0,
      right: 900,
      top: 0,
      width: 900,
      x: 0,
      y: 0,
      toJSON: () => undefined,
    });

    const overlay = container.querySelector(".h-48.w-64") as HTMLElement;
    expect(overlay).toBeTruthy();
    vi.spyOn(overlay, "getBoundingClientRect").mockReturnValue({
      bottom: 192,
      height: 192,
      left: 0,
      right: 256,
      top: 0,
      width: 256,
      x: 0,
      y: 0,
      toJSON: () => undefined,
    });

    act(() => {
      window.dispatchEvent(new Event("resize"));
    });

    await waitFor(() => {
      expect(overlay.style.left).toBe("628px");
      expect(overlay.style.top).toBe("16px");
    });
  });

  it("notifies the page when the camera stream becomes available", async () => {
    const stream = createMockStream();
    const onStreamChange = vi.fn();
    mockGetUserMedia(stream);

    renderOverlay({ onStreamChange });

    await waitFor(() => {
      expect(onStreamChange).toHaveBeenCalledWith(stream);
    });
  });
});

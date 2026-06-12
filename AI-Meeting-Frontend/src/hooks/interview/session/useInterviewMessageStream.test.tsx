import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useInterviewMessageStream } from "@/hooks/interview/session/useInterviewMessageStream";

describe("useInterviewMessageStream", () => {
  it("stores the question number on appended interview questions", async () => {
    const { result } = renderHook(() => useInterviewMessageStream());

    await act(async () => {
      await result.current.appendNextQuestionMessage(
        "请先做一个简短的自我介绍。",
        "1",
        false,
        0,
      );
    });

    const latestMessage = result.current.messages.at(-1);
    expect(latestMessage?.questionNumber).toBe("1");
  });
});

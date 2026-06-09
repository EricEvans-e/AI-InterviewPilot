import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import ChatBubble from "@/components/chat/ChatBubble";
import { CHAT_ROLES } from "@/lib/constants";

describe("ChatBubble", () => {
  it("labels auto-play assistant messages as the current interview question", () => {
    render(
      <ChatBubble
        role={CHAT_ROLES.assistant}
        content="请具体描述一下这两种技术如何协同工作？"
        tts={{ text: "请具体描述一下这两种技术如何协同工作？", autoPlay: true }}
      />,
    );

    expect(screen.getByText("当前题目")).toBeTruthy();
    expect(screen.getByText(/两种技术如何协同工作/)).toBeTruthy();
  });
});

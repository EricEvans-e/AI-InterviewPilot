import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import ChatBubble from "@/components/chat/ChatBubble";
import { CHAT_ROLES } from "@/lib/constants";

describe("ChatBubble", () => {
  it("labels auto-play assistant messages as the current interview question", () => {
    render(
      <ChatBubble
        role={CHAT_ROLES.assistant}
        content="璇峰叿浣撴弿杩颁竴涓嬭繖涓ょ鎶€鏈浣曞崗鍚屽伐浣滐紵"
        tts={{
          text: "璇峰叿浣撴弿杩颁竴涓嬭繖涓ょ鎶€鏈浣曞崗鍚屽伐浣滐紵",
          autoPlay: true,
        }}
      />,
    );

    expect(screen.getByText("当前题目")).toBeTruthy();
    expect(screen.getByText(/涓ょ鎶€鏈浣曞崗鍚屽伐浣?/)).toBeTruthy();
  });

  it("labels the first self-introduction question explicitly", () => {
    render(
      <ChatBubble
        role={CHAT_ROLES.assistant}
        content="请先做一个简短的自我介绍，重点介绍你的教育背景、相关经历，以及你与应聘方向的匹配度。"
        questionNumber="1"
        tts={{
          text: "请先做一个简短的自我介绍，重点介绍你的教育背景、相关经历，以及你与应聘方向的匹配度。",
          autoPlay: true,
        }}
      />,
    );

    expect(screen.getByText("自我介绍")).toBeTruthy();
  });
});

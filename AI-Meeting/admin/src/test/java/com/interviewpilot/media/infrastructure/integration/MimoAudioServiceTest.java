package com.interviewpilot.media.infrastructure.integration;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.interviewpilot.common.config.mimo.MimoCredentialResolver;
import com.interviewpilot.common.config.mimo.MimoProperties;
import com.interviewpilot.common.convention.exception.ServiceException;
import com.interviewpilot.media.api.io.req.LongTextTtsReqDTO;
import com.interviewpilot.media.api.io.resp.LongTextTtsTaskRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MimoAudioServiceTest {

    private MimoProperties properties;
    private MimoAudioService service;

    @BeforeEach
    void setUp() {
        properties = new MimoProperties();
        properties.setApiKey("tp-test-key");
        service = new MimoAudioService(
                properties,
                new MimoCredentialResolver(new MockEnvironment())
        );
    }

    @Test
    void buildAsrRequestBody_ShouldUseMimoAsrInputAudioContract() {
        JSONObject body = service.buildAsrRequestBody(new byte[]{1, 2, 3}, "audio/wav");

        assertEquals("mimo-v2.5-asr", body.getString("model"));
        JSONArray messages = body.getJSONArray("messages");
        assertEquals("user", messages.getJSONObject(0).getString("role"));
        JSONObject inputAudio = messages.getJSONObject(0)
                .getJSONArray("content")
                .getJSONObject(0)
                .getJSONObject("input_audio");
        assertEquals("data:audio/wav;base64,AQID", inputAudio.getString("data"));
        assertEquals("auto", body.getJSONObject("asr_options").getString("language"));
    }

    @Test
    void buildAsrRequestBody_ShouldWrapRawPcmChunksAsWavForMimoAsr() {
        JSONObject body = service.buildAsrRequestBody(new byte[]{0, 0, 1, 0}, "audio/pcm");
        String dataUrl = body.getJSONArray("messages")
                .getJSONObject(0)
                .getJSONArray("content")
                .getJSONObject(0)
                .getJSONObject("input_audio")
                .getString("data");

        assertTrue(dataUrl.startsWith("data:audio/wav;base64,"));
        byte[] decoded = Base64.getDecoder().decode(dataUrl.substring("data:audio/wav;base64,".length()));
        assertEquals("RIFF", new String(decoded, 0, 4, StandardCharsets.US_ASCII));
        assertEquals("WAVE", new String(decoded, 8, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void parseAsrText_ShouldReadAssistantMessageContent() {
        JSONObject response = JSONObject.parseObject("""
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "Good morning."
                      }
                    }
                  ]
                }
                """);

        assertEquals("Good morning.", service.parseAsrText(response));
    }

    @Test
    void parseAsrStreamText_ShouldReadDeltaStringContent() {
        JSONObject chunk = JSONObject.parseObject("""
                {
                  "choices": [
                    {
                      "delta": {
                        "content": "你好"
                      }
                    }
                  ]
                }
                """);

        assertEquals("你好", service.parseAsrStreamText(chunk));
    }

    @Test
    void parseAsrStreamText_ShouldReadDeltaArrayTextBlocks() {
        JSONObject chunk = JSONObject.parseObject("""
                {
                  "choices": [
                    {
                      "delta": {
                        "content": [
                          {
                            "type": "output_text",
                            "text": "杭州"
                          },
                          {
                            "type": "output_text",
                            "text": "科技"
                          }
                        ]
                      }
                    }
                  ]
                }
                """);

        assertEquals("杭州科技", service.parseAsrStreamText(chunk));
    }

    @Test
    void buildTtsRequestBody_ShouldPutTargetTextInAssistantMessageAndAudioOptions() {
        LongTextTtsReqDTO request = new LongTextTtsReqDTO();
        request.setText("你好，欢迎参加模拟面试。");
        request.setVcn("Chloe");

        JSONObject body = service.buildTtsRequestBody(request);

        assertEquals("mimo-v2.5-tts", body.getString("model"));
        JSONArray messages = body.getJSONArray("messages");
        JSONObject assistantMessage = messages.stream()
                .map(JSONObject.class::cast)
                .filter(message -> "assistant".equals(message.getString("role")))
                .findFirst()
                .orElseThrow();
        assertEquals("你好，欢迎参加模拟面试。", assistantMessage.getString("content"));
        assertEquals("wav", body.getJSONObject("audio").getString("format"));
        assertEquals("Chloe", body.getJSONObject("audio").getString("voice"));
    }

    @Test
    void parseTtsResponse_ShouldReadAudioDataFromAssistantMessageAudio() {
        JSONObject response = JSONObject.parseObject("""
                {
                  "id": "chatcmpl-tts-1",
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "audio": {
                          "data": "UklGRg=="
                        }
                      }
                    }
                  ]
                }
                """);

        LongTextTtsTaskRespDTO result = service.parseTtsResponse(response);

        assertEquals(0, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("chatcmpl-tts-1", result.getTaskId());
        assertEquals("5", result.getTaskStatus());
        assertEquals("UklGRg==", result.getAudioBase64());
        assertTrue(result.getCompleted());
        assertTrue(result.getSuccess());
    }

    @Test
    void validateCredentials_ShouldRejectMissingMimoApiKey() {
        properties.setApiKey("");

        assertThrows(ServiceException.class, service::validateCredentials);
    }
}

package com.interviewpilot.media.infrastructure.integration;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.interviewpilot.common.config.mimo.MimoCredentialResolver;
import com.interviewpilot.common.config.mimo.MimoProperties;
import com.interviewpilot.common.convention.exception.ClientException;
import com.interviewpilot.common.convention.exception.ServiceException;
import com.interviewpilot.media.api.io.req.LongTextTtsReqDTO;
import com.interviewpilot.media.api.io.resp.LongTextTtsTaskRespDTO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MimoAudioService {

    private static final int MAX_AUDIO_BYTES = 10 * 1024 * 1024;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    private final MimoProperties properties;
    private final MimoCredentialResolver credentialResolver;

    @Resource(name = "queryExecutor")
    private ExecutorService queryExecutor;

    public CompletableFuture<String> convertAudioToText(MultipartFile audioFile) {
        return CompletableFuture.supplyAsync(() -> {
            if (audioFile == null || audioFile.isEmpty()) {
                throw new ClientException("audio file must not be empty");
            }
            try {
                String contentType = StrUtil.blankToDefault(audioFile.getContentType(), mimeTypeFromFilename(audioFile.getOriginalFilename()));
                return transcribe(audioFile.getBytes(), contentType);
            } catch (IOException ex) {
                throw new ServiceException("Failed to read uploaded audio: " + ex.getMessage());
            }
        }, resolveExecutor());
    }

    public CompletableFuture<String> realTimeAudioToText(InputStream audioInputStream,
                                                         AudioResultCallback callback) {
        if (audioInputStream == null) {
            return CompletableFuture.failedFuture(new ClientException("audioInputStream cannot be null"));
        }
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream in = audioInputStream) {
                byte[] pcmAudio = readAllBytes(in, MAX_AUDIO_BYTES);
                String finalText = transcribe(pcmAudio, "audio/pcm");
                if (callback != null && StrUtil.isNotBlank(finalText)) {
                    callback.onResult(new RealtimeTranscriptionUpdate(
                            finalText,
                            finalText,
                            "",
                            finalText,
                            1,
                            "final",
                            0,
                            finalText,
                            null,
                            null,
                            null,
                            null,
                            true
                    ));
                }
                return finalText;
            } catch (IOException ex) {
                throw new ServiceException("Failed to read streamed audio: " + ex.getMessage());
            }
        }, resolveExecutor());
    }

    public String transcribe(byte[] audioBytes, String mimeType) {
        validateCredentials();
        JSONObject body = buildAsrRequestBody(audioBytes, mimeType);
        JSONObject response = postChatCompletions(body);
        return parseAsrText(response);
    }

    public LongTextTtsTaskRespDTO createTask(LongTextTtsReqDTO requestParam) {
        return synthesizeAndWait(requestParam);
    }

    public LongTextTtsTaskRespDTO queryTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new ClientException("taskId must not be blank");
        }
        LongTextTtsTaskRespDTO result = new LongTextTtsTaskRespDTO();
        result.setTaskId(taskId.trim());
        result.setTaskStatus("5");
        result.setCode(0);
        result.setMessage("Mimo TTS is synchronous; query returns completed only for compatibility");
        result.setCompleted(true);
        result.setSuccess(true);
        return result;
    }

    public LongTextTtsTaskRespDTO synthesizeAndWait(LongTextTtsReqDTO requestParam) {
        validateCredentials();
        validateTtsRequest(requestParam);
        JSONObject body = buildTtsRequestBody(requestParam);
        JSONObject response = postChatCompletions(body);
        LongTextTtsTaskRespDTO result = parseTtsResponse(response);
        if (StrUtil.isBlank(result.getAudioBase64())) {
            throw new ServiceException("Mimo TTS response completed without audio data");
        }
        return result;
    }

    public JSONObject buildAsrRequestBody(byte[] audioBytes, String mimeType) {
        if (audioBytes == null || audioBytes.length == 0) {
            throw new ClientException("audio bytes must not be empty");
        }
        if (audioBytes.length > MAX_AUDIO_BYTES) {
            throw new ClientException("audio payload must not exceed 10MB");
        }

        AudioPayload payload = normalizeAsrPayload(audioBytes, mimeType);
        JSONObject body = new JSONObject();
        body.put("model", StrUtil.blankToDefault(properties.getAsrModel(), "mimo-v2.5-asr"));

        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        JSONArray content = new JSONArray();
        JSONObject audioContent = new JSONObject();
        audioContent.put("type", "input_audio");
        JSONObject inputAudio = new JSONObject();
        inputAudio.put("data", "data:" + payload.mimeType() + ";base64,"
                + Base64.getEncoder().encodeToString(payload.bytes()));
        audioContent.put("input_audio", inputAudio);
        content.add(audioContent);
        userMessage.put("content", content);
        messages.add(userMessage);
        body.put("messages", messages);

        JSONObject asrOptions = new JSONObject();
        asrOptions.put("language", StrUtil.blankToDefault(properties.getAsrLanguage(), "auto"));
        body.put("asr_options", asrOptions);
        return body;
    }

    public String parseAsrText(JSONObject response) {
        JSONObject message = firstChoiceMessage(response);
        if (message == null) {
            return "";
        }
        Object content = message.get("content");
        if (content instanceof String text) {
            return text.trim();
        }
        if (content instanceof JSONArray blocks) {
            StringBuilder text = new StringBuilder();
            for (Object blockObject : blocks) {
                if (blockObject instanceof JSONObject block && StrUtil.isNotBlank(block.getString("text"))) {
                    text.append(block.getString("text"));
                }
            }
            return text.toString().trim();
        }
        return "";
    }

    public JSONObject buildTtsRequestBody(LongTextTtsReqDTO requestParam) {
        validateTtsRequest(requestParam);

        JSONObject body = new JSONObject();
        body.put("model", StrUtil.blankToDefault(properties.getTtsModel(), "mimo-v2.5-tts"));

        JSONArray messages = new JSONArray();
        String styleInstruction = buildStyleInstruction(requestParam);
        if (StrUtil.isNotBlank(styleInstruction)) {
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", styleInstruction);
            messages.add(userMessage);
        }

        JSONObject assistantMessage = new JSONObject();
        assistantMessage.put("role", "assistant");
        assistantMessage.put("content", requestParam.getText().trim());
        messages.add(assistantMessage);
        body.put("messages", messages);

        JSONObject audio = new JSONObject();
        audio.put("format", resolveTtsFormat(requestParam));
        audio.put("voice", StrUtil.blankToDefault(requestParam.getVcn(), properties.getTtsVoice()));
        body.put("audio", audio);
        return body;
    }

    public LongTextTtsTaskRespDTO parseTtsResponse(JSONObject response) {
        LongTextTtsTaskRespDTO result = new LongTextTtsTaskRespDTO();
        result.setSid(response != null ? response.getString("id") : null);
        result.setTaskId(StrUtil.blankToDefault(result.getSid(), "mimo-tts-" + UUID.randomUUID()));
        result.setTaskStatus("5");
        result.setCode(0);
        result.setMessage("success");
        result.setCompleted(true);
        result.setSuccess(true);

        JSONObject message = firstChoiceMessage(response);
        JSONObject audio = message != null ? message.getJSONObject("audio") : null;
        if (audio != null) {
            result.setAudioBase64(audio.getString("data"));
        }
        return result;
    }

    public void validateCredentials() {
        if (StrUtil.isBlank(resolveApiKey())) {
            throw new ServiceException("Mimo API key is missing: configure MIMO_API_KEY or mimo.api-key");
        }
    }

    private void validateTtsRequest(LongTextTtsReqDTO requestParam) {
        if (requestParam == null || StrUtil.isBlank(requestParam.getText())) {
            throw new ClientException("text must not be blank");
        }
        if (requestParam.getText().length() > 100000) {
            throw new ClientException("text length must not exceed 100000 characters");
        }
        validateRange("speed", requestParam.getSpeed(), 0, 100);
        validateRange("volume", requestParam.getVolume(), 0, 100);
        validateRange("pitch", requestParam.getPitch(), 0, 100);
    }

    private JSONObject postChatCompletions(JSONObject body) {
        String apiKey = resolveApiKey();
        String baseUrl = StrUtil.blankToDefault(properties.getOpenaiBaseUrl(), MimoProperties.DEFAULT_OPENAI_BASE_URL);
        String url = trimTrailingSlash(baseUrl) + "/chat/completions";
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toJSONString(), JSON_MEDIA_TYPE))
                .addHeader("api-key", apiKey)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Mimo audio request failed, status={}, body={}", response.code(), responseBody);
                throw new ServiceException("Mimo audio request failed, HTTP status: " + response.code());
            }
            if (StrUtil.isBlank(responseBody)) {
                throw new ServiceException("Mimo audio response is empty");
            }
            return JSONObject.parseObject(responseBody);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("Failed to call Mimo audio API: " + ex.getMessage());
        }
    }

    private String resolveApiKey() {
        return credentialResolver.resolveSecret(properties.getApiKey());
    }

    private AudioPayload normalizeAsrPayload(byte[] audioBytes, String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        if ("audio/pcm".equals(normalized) || "application/octet-stream".equals(normalized)) {
            return new AudioPayload(wrapPcm16AsWav(audioBytes), "audio/wav");
        }
        return new AudioPayload(audioBytes, normalized);
    }

    private byte[] wrapPcm16AsWav(byte[] pcmAudio) {
        int sampleRate = valueOrDefault(properties.getPcmSampleRate(), 16000);
        int channels = valueOrDefault(properties.getPcmChannels(), 1);
        int bitsPerSample = valueOrDefault(properties.getPcmBitsPerSample(), 16);
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcmAudio.length;

        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        header.putInt(36 + dataSize);
        header.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        header.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        header.putInt(16);
        header.putShort((short) 1);
        header.putShort((short) channels);
        header.putInt(sampleRate);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) bitsPerSample);
        header.put("data".getBytes(StandardCharsets.US_ASCII));
        header.putInt(dataSize);

        byte[] wav = new byte[44 + dataSize];
        System.arraycopy(header.array(), 0, wav, 0, 44);
        System.arraycopy(pcmAudio, 0, wav, 44, dataSize);
        return wav;
    }

    private String normalizeMimeType(String mimeType) {
        String normalized = StrUtil.blankToDefault(mimeType, "audio/pcm").trim().toLowerCase();
        int semicolon = normalized.indexOf(';');
        if (semicolon >= 0) {
            normalized = normalized.substring(0, semicolon);
        }
        if ("audio/x-wav".equals(normalized) || "audio/wave".equals(normalized)) {
            return "audio/wav";
        }
        return normalized;
    }

    private String mimeTypeFromFilename(String filename) {
        if (filename == null) {
            return "audio/pcm";
        }
        String normalized = filename.toLowerCase();
        if (normalized.endsWith(".wav")) {
            return "audio/wav";
        }
        if (normalized.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (normalized.endsWith(".flac")) {
            return "audio/flac";
        }
        if (normalized.endsWith(".pcm")) {
            return "audio/pcm";
        }
        return "audio/pcm";
    }

    private String resolveTtsFormat(LongTextTtsReqDTO requestParam) {
        String encoding = requestParam.getAudioEncoding();
        if (StrUtil.isBlank(encoding)) {
            return StrUtil.blankToDefault(properties.getTtsFormat(), "wav");
        }
        String normalized = encoding.trim().toLowerCase();
        if ("lame".equals(normalized) || "mp3".equals(normalized) || "mpeg".equals(normalized)) {
            return "mp3";
        }
        if ("pcm".equals(normalized) || "pcm16".equals(normalized)) {
            return "pcm16";
        }
        return "wav";
    }

    private String buildStyleInstruction(LongTextTtsReqDTO requestParam) {
        StringBuilder builder = new StringBuilder();
        if (StrUtil.isNotBlank(requestParam.getLanguage())) {
            builder.append("Language: ").append(requestParam.getLanguage().trim()).append(". ");
        }
        if (requestParam.getSpeed() != null) {
            builder.append("Speed: ").append(requestParam.getSpeed()).append("/100. ");
        }
        if (requestParam.getPitch() != null) {
            builder.append("Pitch: ").append(requestParam.getPitch()).append("/100. ");
        }
        if (requestParam.getVolume() != null) {
            builder.append("Volume: ").append(requestParam.getVolume()).append("/100. ");
        }
        return builder.toString().trim();
    }

    private JSONObject firstChoiceMessage(JSONObject response) {
        JSONArray choices = response != null ? response.getJSONArray("choices") : null;
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        JSONObject choice = choices.getJSONObject(0);
        return choice != null ? choice.getJSONObject("message") : null;
    }

    private byte[] readAllBytes(InputStream inputStream, int maxBytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new ClientException("audio payload must not exceed 10MB");
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private ExecutorService resolveExecutor() {
        return queryExecutor != null ? queryExecutor : java.util.concurrent.ForkJoinPool.commonPool();
    }

    private void validateRange(String fieldName, Integer value, int minInclusive, int maxInclusive) {
        if (value == null) {
            return;
        }
        if (value < minInclusive || value > maxInclusive) {
            throw new ClientException(fieldName + " must be between " + minInclusive + " and " + maxInclusive);
        }
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private record AudioPayload(byte[] bytes, String mimeType) {
    }

    public interface AudioResultCallback {
        void onResult(RealtimeTranscriptionUpdate result);
    }

    public record RealtimeTranscriptionUpdate(String fullText,
                                              String committedText,
                                              String liveText,
                                              String displayText,
                                              Integer revision,
                                              String resultStatus,
                                              Integer segmentId,
                                              String segmentText,
                                              String pgs,
                                              int[] rg,
                                              Integer bg,
                                              Integer ed,
                                              boolean finalPacket) {
    }
}

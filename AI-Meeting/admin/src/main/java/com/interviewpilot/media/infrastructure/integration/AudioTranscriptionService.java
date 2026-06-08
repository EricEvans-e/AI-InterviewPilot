package com.interviewpilot.media.infrastructure.integration;

import com.interviewpilot.common.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioTranscriptionService {

    private final MimoAudioService mimoAudioService;

    public CompletableFuture<String> transcribeAsync(MultipartFile audioFile,
                                                     Consumer<String> partialResultCallback) {
        return mimoAudioService.convertAudioToText(audioFile)
                .thenApply(result -> {
                    if (partialResultCallback != null) {
                        partialResultCallback.accept(result);
                    }
                    return result;
                });
    }

    public String transcribeAudio(MultipartFile audioFile) throws Exception {
        return transcribeSync(audioFile, null);
    }

    public String transcribeSync(MultipartFile audioFile,
                                 Consumer<String> partialResultCallback) throws Exception {
        validateAudioFile(audioFile);
        String result = mimoAudioService.convertAudioToText(audioFile).get();
        if (partialResultCallback != null) {
            partialResultCallback.accept(result);
        }
        log.info("Mimo audio transcription completed, file={}, resultLength={}",
                audioFile.getOriginalFilename(), result != null ? result.length() : 0);
        return result;
    }

    public void transcribeWithCallback(MultipartFile audioFile,
                                       AudioTranscriptionCallback callback) {
        try {
            validateAudioFile(audioFile);
            mimoAudioService.convertAudioToText(audioFile).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    callback.onError(throwable instanceof Exception exception
                            ? exception
                            : new RuntimeException(throwable));
                    return;
                }
                callback.onPartialResult(result);
                callback.onSuccess(result);
            });
        } catch (Exception ex) {
            log.error("Failed to start Mimo async audio transcription", ex);
            callback.onError(ex);
        }
    }

    private void validateAudioFile(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new ClientException("audio file must not be empty");
        }
        long maxSize = 10L * 1024 * 1024;
        if (audioFile.getSize() > maxSize) {
            throw new ClientException("audio file size must not exceed 10MB");
        }
    }

    public interface AudioTranscriptionCallback {

        void onSuccess(String result);

        void onError(Exception error);

        default void onPartialResult(String partialResult) {
        }
    }
}

package com.interviewpilot.media.infrastructure.websocket;

import com.alibaba.fastjson2.JSON;
import com.interviewpilot.auth.application.WebSocketAuthService;
import com.interviewpilot.media.infrastructure.integration.MimoAudioService;
import com.interviewpilot.media.infrastructure.integration.MimoAudioService.RealtimeTranscriptionUpdate;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Real-time speech-to-text WebSocket endpoint.
 */
@Slf4j
@Component
@ServerEndpoint(value = "/api/ip/v1/mimo/audio-to-text/{userId}")
public class AudioTranscriptionWebSocketHandler {

    private static final int WEBSOCKET_MESSAGE_BUFFER_BYTES = 64 * 1024;
    private static final long TRANSCRIPTION_FLUSH_INTERVAL_MS = 700L;

    private static volatile MimoAudioService mimoAudioService;
    private static volatile WebSocketAuthService webSocketAuthService;
    private static volatile ScheduledExecutorService heartbeatExecutor;

    @Autowired
    public void setMimoAudioService(MimoAudioService service) {
        AudioTranscriptionWebSocketHandler.mimoAudioService = service;
    }

    @Autowired
    public void setWebSocketAuthService(WebSocketAuthService service) {
        AudioTranscriptionWebSocketHandler.webSocketAuthService = service;
    }

    @Autowired
    public void setHeartbeatExecutor(@Qualifier("scheduledExecutorService") ScheduledExecutorService scheduledExecutorService) {
        AudioTranscriptionWebSocketHandler.heartbeatExecutor = scheduledExecutorService;
    }

    private static final ConcurrentMap<String, Session> USER_SESSIONS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> SESSION_USER_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, TranscriptionSessionContext> TRANSCRIPTION_CONTEXTS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ScheduledFuture<?>> HEARTBEAT_TASKS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        configureSessionBuffers(session);

        if (!isAuthorizedUser(session, userId)) {
            log.warn("WebSocket auth failed, userId={}, sessionId={}", userId, session.getId());
            closeSession(session, "Unauthorized websocket connection");
            return;
        }

        String sessionId = session.getId();
        USER_SESSIONS.put(userId, session);
        SESSION_USER_MAP.put(sessionId, userId);
        log.info("WebSocket connected, userId={}, sessionId={}", userId, sessionId);

        sendMessage(session, createResponse("connected", "WebSocket connected", userId));
        startHeartbeat(session);
    }

    void configureSessionBuffers(Session session) {
        if (session == null) {
            return;
        }
        session.setMaxBinaryMessageBufferSize(WEBSOCKET_MESSAGE_BUFFER_BYTES);
        session.setMaxTextMessageBufferSize(WEBSOCKET_MESSAGE_BUFFER_BYTES);
    }

    private boolean isAuthorizedUser(Session session, String pathUserId) {
        if (webSocketAuthService == null) {
            log.error("WebSocketAuthService is not injected, reject websocket connection");
            return false;
        }
        return webSocketAuthService.isAuthorized(session, pathUserId);
    }

    private void closeSession(Session session, String reason) {
        if (session == null) {
            return;
        }
        try {
            if (session.isOpen()) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
            }
        } catch (IOException ex) {
            log.warn("Failed to close websocket session, sessionId={}", session.getId(), ex);
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        String userId = SESSION_USER_MAP.get(session.getId());
        log.info("Received text message, userId={}, message={}", userId, message);

        try {
            WebSocketMessage wsMessage = JSON.parseObject(message, WebSocketMessage.class);
            handleControlMessage(session, userId, wsMessage);
        } catch (Exception ex) {
            sendMessage(session, createResponse("info", "Received text message: " + message, null));
        }
    }

    @OnMessage
    public void onMessage(Session session, ByteBuffer byteBuffer) {
        String sessionId = session.getId();
        String userId = SESSION_USER_MAP.get(sessionId);
        log.debug("Received audio chunk, userId={}, sessionId={}, bytes={}",
                userId, sessionId, byteBuffer.remaining());

        try {
            byte[] audioData = new byte[byteBuffer.remaining()];
            byteBuffer.get(audioData);

            TranscriptionSessionContext context = TRANSCRIPTION_CONTEXTS.get(sessionId);
            if (context == null || !context.active.get()) {
                log.warn("Audio chunk received before transcription session started, userId={}, sessionId={}",
                        userId, sessionId);
                sendMessage(session, createResponse("error",
                        "Transcription session is not started. Send start_transcription first.", null));
                return;
            }

            context.appendAudio(audioData);
        } catch (Exception ex) {
            log.error("Failed to process audio chunk, userId={}, sessionId={}", userId, sessionId, ex);
            sendMessage(session, createResponse("error", "Failed to process audio chunk: " + ex.getMessage(), null));
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        String sessionId = session.getId();
        String userId = SESSION_USER_MAP.get(sessionId);

        stopTranscriptionSession(sessionId);
        cancelHeartbeat(sessionId);

        if (userId != null) {
            USER_SESSIONS.remove(userId);
            SESSION_USER_MAP.remove(sessionId);
        }
        String reason = closeReason != null ? closeReason.getReasonPhrase() : "unknown";
        log.info("WebSocket closed, userId={}, sessionId={}, reason={}",
                userId, sessionId, reason);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        String sessionId = session != null ? session.getId() : null;
        String userId = sessionId != null ? SESSION_USER_MAP.get(sessionId) : null;
        log.error("WebSocket error, userId={}, sessionId={}", userId, sessionId, error);

        if (sessionId != null) {
            stopTranscriptionSession(sessionId);
            cancelHeartbeat(sessionId);
        }
        sendMessage(session, createResponse("error", "WebSocket error: " + error.getMessage(), null));
    }

    private void handleControlMessage(Session session, String userId, WebSocketMessage message) {
        String type = message != null ? message.getType() : null;
        if (type == null) {
            sendMessage(session, createResponse("unknown_command", "Missing command type", null));
            return;
        }

        switch (type) {
            case "ping" -> sendMessage(session, createResponse("pong", "pong", String.valueOf(System.currentTimeMillis())));
            case "start_transcription" -> startTranscriptionSession(session, userId);
            case "stop_transcription" -> {
                boolean stopped = stopTranscriptionSession(session.getId());
                if (stopped) {
                    sendMessage(session, createResponse("transcription_stopped", "Transcription stopped", null));
                } else {
                    sendMessage(session, createResponse("transcription_already_stopped",
                            "Transcription is already stopped", null));
                }
            }
            case "get_status" -> sendMessage(session, createResponse("status", "Connection is healthy", userId));
            default -> sendMessage(session, createResponse("unknown_command", "Unknown command: " + type, null));
        }
    }

    private void startHeartbeat(Session session) {
        if (heartbeatExecutor == null) {
            log.warn("scheduledExecutorService is not injected, skip heartbeat, sessionId={}", session.getId());
            return;
        }
        String sessionId = session.getId();
        ScheduledFuture<?> oldTask = HEARTBEAT_TASKS.remove(sessionId);
        if (oldTask != null) {
            oldTask.cancel(true);
        }

        ScheduledFuture<?> task = heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (session.isOpen()) {
                sendMessage(session, createResponse("heartbeat", "heartbeat", String.valueOf(System.currentTimeMillis())));
            }
        }, 30, 30, TimeUnit.SECONDS);
        HEARTBEAT_TASKS.put(sessionId, task);
    }

    private void cancelHeartbeat(String sessionId) {
        ScheduledFuture<?> task = HEARTBEAT_TASKS.remove(sessionId);
        if (task != null) {
            task.cancel(true);
        }
    }

    private void startTranscriptionSession(Session session, String userId) {
        String sessionId = session.getId();
        TranscriptionSessionContext existing = TRANSCRIPTION_CONTEXTS.get(sessionId);
        if (existing != null && existing.active.get() && !existing.stopRequested.get()) {
            sendMessage(session, createResponse("transcription_already_started",
                    "Transcription is already started", null));
            return;
        }

        stopTranscriptionSession(sessionId);

        TranscriptionSessionContext context = createAndStartTranscriptionSession(session, userId);
        if (context != null) {
            TranscriptionSessionContext raced = TRANSCRIPTION_CONTEXTS.putIfAbsent(sessionId, context);
            if (raced != null && raced.active.get() && !raced.stopRequested.get()) {
                context.active.set(false);
                context.stopRequested.set(true);
                cancelTranscriptionFlush(context);
                sendMessage(session, createResponse("transcription_already_started",
                        "Transcription is already started", null));
                return;
            }
            TRANSCRIPTION_CONTEXTS.put(sessionId, context);
            sendMessage(session, createResponse("transcription_started", "Transcription started", null));
        } else {
            sendMessage(session, createResponse("error", "Failed to start transcription", null));
        }
    }

    private TranscriptionSessionContext createAndStartTranscriptionSession(Session session, String userId) {
        String sessionId = session.getId();
        try {
            if (mimoAudioService == null) {
                log.error("MimoAudioService is not injected yet, cannot start transcription. sessionId={}", sessionId);
                return null;
            }
            AtomicBoolean active = new AtomicBoolean(true);
            TranscriptionSessionContext context = new TranscriptionSessionContext(active);
            scheduleTranscriptionFlush(session, userId, sessionId, context);
            return context;
        } catch (Exception ex) {
            log.error("Failed to create transcription session, userId={}, sessionId={}", userId, sessionId, ex);
            return null;
        }
    }

    private boolean stopTranscriptionSession(String sessionId) {
        TranscriptionSessionContext context = TRANSCRIPTION_CONTEXTS.remove(sessionId);
        if (context == null) {
            return false;
        }
        context.active.set(false);
        context.stopRequested.set(true);
        cancelTranscriptionFlush(context);
        return true;
    }

    private void cleanupTranscriptionContext(String sessionId, TranscriptionSessionContext context) {
        TRANSCRIPTION_CONTEXTS.remove(sessionId, context);
        context.active.set(false);
        cancelTranscriptionFlush(context);
    }

    private void scheduleTranscriptionFlush(Session session,
                                            String userId,
                                            String sessionId,
                                            TranscriptionSessionContext context) {
        if (heartbeatExecutor == null) {
            log.warn("scheduledExecutorService is not injected, skip transcription flush, sessionId={}", sessionId);
            return;
        }
        ScheduledFuture<?> task = heartbeatExecutor.scheduleAtFixedRate(
                () -> flushTranscriptionSnapshot(session, userId, sessionId, context, false),
                TRANSCRIPTION_FLUSH_INTERVAL_MS,
                TRANSCRIPTION_FLUSH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        context.flushTask.set(task);
    }

    private void flushTranscriptionSnapshot(Session session,
                                            String userId,
                                            String sessionId,
                                            TranscriptionSessionContext context,
                                            boolean finalize) {
        if (context == null) {
            return;
        }
        byte[] snapshot = context.snapshotAudio();
        if (snapshot.length == 0) {
            if (finalize) {
                cleanupTranscriptionContext(sessionId, context);
            }
            return;
        }
        if (!context.inFlight.compareAndSet(false, true)) {
            return;
        }
        CompletableFuture.supplyAsync(() -> mimoAudioService.streamAudioToText(snapshot, "audio/pcm", update -> {
                    context.lastUpdate.set(update);
                    sendMessage(session, createResponse("transcription", "Partial snapshot", update, true));
                }))
                .whenComplete((finalResult, throwable) -> {
                    context.inFlight.set(false);
                    if (throwable != null) {
                        if (!context.stopRequested.get()) {
                            log.error("Transcription failed, userId={}, sessionId={}", userId, sessionId, throwable);
                            sendMessage(session, createResponse("error",
                                    "Transcription failed: " + throwable.getMessage(), null));
                        }
                        if (finalize) {
                            cleanupTranscriptionContext(sessionId, context);
                        }
                        return;
                    }

                    if (finalize && finalResult != null) {
                        sendMessage(session, createResponse("final", "Transcription completed",
                                buildFinalUpdate(finalResult, context.lastUpdate.get()), true));
                    }
                    if (finalize) {
                        cleanupTranscriptionContext(sessionId, context);
                    }
                });
    }

    private void cancelTranscriptionFlush(TranscriptionSessionContext context) {
        ScheduledFuture<?> task = context != null ? context.flushTask.getAndSet(null) : null;
        if (task != null) {
            task.cancel(true);
        }
    }

    private void sendMessage(Session session, String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException ex) {
                log.error("Failed to send message, sessionId={}", session.getId(), ex);
            }
        }
    }

    public static void sendMessageToUser(String userId, String type, String message, String data) {
        Session session = USER_SESSIONS.get(userId);
        if (session == null || !session.isOpen()) {
            log.warn("User is offline, userId={}", userId);
            return;
        }
        try {
            session.getBasicRemote().sendText(createStaticResponse(type, message, data));
        } catch (IOException ex) {
            log.error("Failed to send message to user, userId={}", userId, ex);
        }
    }

    public static void broadcastMessage(String type, String message, String data) {
        String payload = createStaticResponse(type, message, data);
        USER_SESSIONS.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(payload);
                } catch (IOException ex) {
                    log.error("Broadcast failed, userId={}", userId, ex);
                }
            }
        });
    }

    public static Set<String> getOnlineUsers() {
        return USER_SESSIONS.keySet();
    }

    public static boolean isUserOnline(String userId) {
        Session session = USER_SESSIONS.get(userId);
        return session != null && session.isOpen();
    }

    private String createResponse(String type, String message, String data) {
        return createResponse(type, message, data, false);
    }

    private String createResponse(String type, String message, String data, boolean isSnapshot) {
        WebSocketResponse response = new WebSocketResponse();
        response.setType(type);
        response.setMessage(message);
        response.setData(data);
        response.setFullText(resolveFullText(type, data));
        response.setIsSnapshot(isSnapshot);
        response.setUpdateAction(resolveUpdateAction(type));
        response.setTimestamp(System.currentTimeMillis());
        return JSON.toJSONString(response);
    }

    private String createResponse(String type,
                                  String message,
                                  RealtimeTranscriptionUpdate update,
                                  boolean isSnapshot) {
        WebSocketResponse response = new WebSocketResponse();
        response.setType(type);
        response.setMessage(message);
        response.setData(update != null ? update.fullText() : null);
        response.setFullText(update != null ? update.fullText() : null);
        response.setDisplayText(update != null ? update.displayText() : null);
        response.setCommittedText(update != null ? update.committedText() : null);
        response.setLiveText(update != null ? update.liveText() : null);
        response.setRevision(update != null ? update.revision() : null);
        response.setResultStatus(update != null ? update.resultStatus() : null);
        response.setIsSnapshot(isSnapshot);
        response.setUpdateAction(resolveUpdateAction(type));
        response.setTimestamp(System.currentTimeMillis());
        if (update != null) {
            response.setSegmentId(update.segmentId());
            response.setSentenceSeq(update.segmentId());
            response.setSegmentText(update.segmentText());
            response.setPgs(update.pgs());
            response.setRg(update.rg());
            response.setBg(update.bg());
            response.setEd(update.ed());
            response.setIsFinalPacket(update.finalPacket());
        }
        return JSON.toJSONString(response);
    }

    private RealtimeTranscriptionUpdate buildFinalUpdate(String finalResult,
                                                         RealtimeTranscriptionUpdate lastUpdate) {
        if (lastUpdate == null) {
            return new RealtimeTranscriptionUpdate(
                    finalResult,
                    finalResult,
                    "",
                    finalResult,
                    1,
                    "final",
                    0,
                    finalResult,
                    null,
                    null,
                    null,
                    null,
                    true
            );
        }
        return new RealtimeTranscriptionUpdate(
                finalResult,
                finalResult,
                "",
                finalResult,
                lastUpdate.revision() != null ? lastUpdate.revision() + 1 : 1,
                "final",
                lastUpdate.segmentId(),
                lastUpdate.segmentText(),
                lastUpdate.pgs(),
                lastUpdate.rg(),
                lastUpdate.bg(),
                lastUpdate.ed(),
                true
        );
    }

    private static String createStaticResponse(String type, String message, String data) {
        WebSocketResponse response = new WebSocketResponse();
        response.setType(type);
        response.setMessage(message);
        response.setData(data);
        response.setFullText(resolveFullText(type, data));
        response.setIsSnapshot(false);
        response.setUpdateAction(resolveUpdateAction(type));
        response.setTimestamp(System.currentTimeMillis());
        return JSON.toJSONString(response);
    }

    private static String resolveFullText(String type, String data) {
        if ("transcription".equals(type) || "final".equals(type)) {
            return data;
        }
        return null;
    }

    private static String resolveUpdateAction(String type) {
        if ("transcription".equals(type)) {
            return "replace";
        }
        if ("final".equals(type)) {
            return "archive";
        }
        return "none";
    }

    @Data
    public static class WebSocketResponse {
        private String type;
        private String message;
        private String data;
        private String fullText;
        private String displayText;
        private String committedText;
        private String liveText;
        private Integer revision;
        private String resultStatus;
        private Boolean isSnapshot;
        private String updateAction;
        private Long timestamp;
        private Integer segmentId;
        private Integer sentenceSeq;
        private String segmentText;
        private String pgs;
        private int[] rg;
        private Integer bg;
        private Integer ed;
        private Boolean isFinalPacket;
    }

    @Data
    public static class WebSocketMessage {
        private String type;
    }

    private static class TranscriptionSessionContext {
        private final AtomicBoolean active;
        private final AtomicBoolean stopRequested = new AtomicBoolean(false);
        private final AtomicReference<RealtimeTranscriptionUpdate> lastUpdate = new AtomicReference<>();
        private final AtomicReference<ScheduledFuture<?>> flushTask = new AtomicReference<>();
        private final AtomicBoolean inFlight = new AtomicBoolean(false);
        private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();

        private TranscriptionSessionContext(AtomicBoolean active) {
            this.active = active;
        }

        private synchronized void appendAudio(byte[] audioData) throws IOException {
            audioBuffer.write(audioData);
        }

        private synchronized byte[] snapshotAudio() {
            return audioBuffer.toByteArray();
        }
    }
}

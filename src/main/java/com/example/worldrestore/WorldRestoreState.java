package com.example.worldrestore;

import java.time.Instant;
import java.util.Optional;

public final class WorldRestoreState {
    private static volatile boolean lastSuccess = false;
    private static volatile boolean lastSkipped = false;
    private static volatile String lastMessage = "No restore has run yet.";
    private static volatile long lastDurationMs = 0L;
    private static volatile Instant lastRun = null;

    private WorldRestoreState() {
    }

    public static void recordSuccess(String message, long durationMs) {
        lastSuccess = true;
        lastSkipped = false;
        lastMessage = message;
        lastDurationMs = durationMs;
        lastRun = Instant.now();
    }

    public static void recordFailure(String message, long durationMs) {
        lastSuccess = false;
        lastSkipped = false;
        lastMessage = message;
        lastDurationMs = durationMs;
        lastRun = Instant.now();
    }

    public static void recordSkipped(String message) {
        lastSuccess = false;
        lastSkipped = true;
        lastMessage = message;
        lastDurationMs = 0L;
        lastRun = Instant.now();
    }

    public static boolean wasLastSuccess() {
        return lastSuccess;
    }

    public static boolean wasLastSkipped() {
        return lastSkipped;
    }

    public static String getLastMessage() {
        return lastMessage;
    }

    public static long getLastDurationMs() {
        return lastDurationMs;
    }

    public static Optional<Instant> getLastRun() {
        return Optional.ofNullable(lastRun);
    }
}

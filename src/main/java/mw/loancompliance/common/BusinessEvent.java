package mw.loancompliance.common;

public record BusinessEvent(
        String actorEmail,
        String action,
        String entityType,
        Long entityId,
        String description,
        Long notificationUserId,
        String notificationTitle,
        String notificationMessage) {
}

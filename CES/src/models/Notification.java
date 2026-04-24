package src.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a system-generated notification delivered to a user.
 */
public class Notification {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final String notificationId;
    private final String userId;
    private final String subject;
    private final String message;
    private final LocalDateTime createdAt;
    private boolean isRead;

    public Notification(String notificationId, String userId,
                        String subject, String message) {
        this.notificationId = notificationId;
        this.userId         = userId;
        this.subject        = subject;
        this.message        = message;
        this.createdAt      = LocalDateTime.now();
        this.isRead         = false;
    }

    // ─── Getters ─────────────────────────────────────────────────────────────────
    public String        getNotificationId() { return notificationId; }
    public String        getUserId()         { return userId; }
    public String        getSubject()        { return subject; }
    public String        getMessage()        { return message; }
    public LocalDateTime getCreatedAt()      { return createdAt; }
    public boolean       isRead()            { return isRead; }

    public void markAsRead() { this.isRead = true; }

    public String getFormattedDate() {
        return createdAt.format(FMT);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s",
                isRead ? "READ" : "NEW", getFormattedDate(), subject, message);
    }
}

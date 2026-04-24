package src.services;

import src.models.Notification;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages the creation, delivery, and retrieval of system notifications.
 * Simulates automated email/push alerts for event lifecycle events.
 */
public class NotificationService {

    private final Map<String, Notification>       notificationsById = new LinkedHashMap<>();
    private final Map<String, List<Notification>> notificationsByUser = new HashMap<>();

    private final AtomicInteger idCounter = new AtomicInteger(1);

    // ─── Create & send ────────────────────────────────────────────────────────────

    /**
     * Send a notification to a single user.
     */
    public Notification sendNotification(String userId, String subject, String message) {
        String id = "N" + String.format("%04d", idCounter.getAndIncrement());
        Notification notification = new Notification(id, userId, subject, message);

        notificationsById.put(id, notification);
        notificationsByUser.computeIfAbsent(userId, k -> new ArrayList<>()).add(notification);

        // Simulate console delivery (replace with real email/SMS in production)
        System.out.println("\n  📧 [NOTIFICATION → " + userId + "] " + subject);
        return notification;
    }

    /**
     * Broadcast the same notification to multiple users.
     */
    public void broadcastNotification(List<String> userIds, String subject, String message) {
        for (String userId : userIds) {
            sendNotification(userId, subject, message);
        }
    }

    // ─── Retrieval ────────────────────────────────────────────────────────────────

    /** All notifications for a user, newest first. */
    public List<Notification> getNotificationsForUser(String userId) {
        List<Notification> list = notificationsByUser.getOrDefault(userId, Collections.emptyList());
        List<Notification> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }

    /** Unread notifications for a user. */
    public List<Notification> getUnreadNotifications(String userId) {
        return getNotificationsForUser(userId).stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());
    }

    public int getUnreadCount(String userId) {
        return (int) getNotificationsForUser(userId).stream()
                .filter(n -> !n.isRead())
                .count();
    }

    /** Mark a specific notification as read. */
    public boolean markAsRead(String notificationId) {
        Notification n = notificationsById.get(notificationId);
        if (n == null) return false;
        n.markAsRead();
        return true;
    }

    /** Mark ALL notifications for a user as read. */
    public void markAllAsRead(String userId) {
        getNotificationsForUser(userId).forEach(Notification::markAsRead);
    }

    // ─── Pre-built notification templates ────────────────────────────────────────

    public void notifyRegistrationConfirmed(String userId, String eventTitle) {
        sendNotification(userId,
                "Registration Confirmed: " + eventTitle,
                "You have successfully registered for '" + eventTitle +
                "'. We look forward to seeing you there!");
    }

    public void notifyAddedToWaitlist(String userId, String eventTitle, int position) {
        sendNotification(userId,
                "Waitlist Position #" + position + ": " + eventTitle,
                "The event '" + eventTitle + "' is currently full. You are #" + position +
                " on the waitlist. You will be notified if a spot becomes available.");
    }

    public void notifyPromotedFromWaitlist(String userId, String eventTitle) {
        sendNotification(userId,
                "🎉 Spot Available: " + eventTitle,
                "Great news! A spot has opened up for '" + eventTitle +
                "' and you have been automatically moved from the waitlist to confirmed attendees.");
    }

    public void notifyRegistrationCancelled(String userId, String eventTitle) {
        sendNotification(userId,
                "Registration Cancelled: " + eventTitle,
                "Your registration for '" + eventTitle + "' has been cancelled successfully.");
    }

    public void notifyEventCancelled(List<String> userIds, String eventTitle) {
        broadcastNotification(userIds,
                "⚠️ Event Cancelled: " + eventTitle,
                "We regret to inform you that the event '" + eventTitle +
                "' has been cancelled. We apologise for any inconvenience caused.");
    }

    public void notifyEventUpdated(List<String> userIds, String eventTitle, String changeDetails) {
        broadcastNotification(userIds,
                "Event Updated: " + eventTitle,
                "The event '" + eventTitle + "' has been updated. Changes: " + changeDetails +
                ". Please review the updated details.");
    }

    public void notifyEventReminder(List<String> userIds, String eventTitle, String startTime) {
        broadcastNotification(userIds,
                "⏰ Reminder: " + eventTitle + " starts soon",
                "This is a reminder that '" + eventTitle + "' is scheduled to begin at " +
                startTime + ". Please make sure to attend!");
    }

    public void notifyNewEventCreated(List<String> userIds, String eventTitle, String category) {
        broadcastNotification(userIds,
                "New Event: " + eventTitle,
                "A new " + category + " event '" + eventTitle +
                "' has been added to the campus calendar. Register now to secure your spot!");
    }

    public int getTotalNotificationCount() { return notificationsById.size(); }
}

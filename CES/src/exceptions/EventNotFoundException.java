package src.exceptions;

/**
 * Thrown when an operation references an Event ID that does not exist.
 */
public class EventNotFoundException extends Exception {
    private final int eventId;

    public EventNotFoundException(int eventId) {
        super("No event found with ID: " + eventId);
        this.eventId = eventId;
    }

    public int getEventId() { return eventId; }
}

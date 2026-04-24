package src.models;

/**
 * Categories for campus events.
 */
public enum EventCategory {
    ACADEMIC("Academic"),
    SPORTS("Sports"),
    CULTURAL("Cultural"),
    WORKSHOP("Workshop"),
    SEMINAR("Seminar"),
    SOCIAL("Social"),
    CAREER("Career"),
    OTHER("Other");

    private final String displayName;

    EventCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static EventCategory fromIndex(int index) {
        EventCategory[] values = EventCategory.values();
        if (index >= 0 && index < values.length) {
            return values[index];
        }
        throw new IllegalArgumentException("Invalid category index: " + index);
    }
}

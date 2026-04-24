package src.models;

/**
 * The two user roles supported by the Campus Event Management System.
 * - STAFF  : can create, update, cancel events and view participants/waitlists
 * - STUDENT: can view events, register, cancel own registration, view own status
 */
public enum Role {
    STAFF("Staff"),
    STUDENT("Student");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

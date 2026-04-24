package src.services;

import src.models.Role;
import src.models.*;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the current session user.
 *
 * Creates the correct concrete subclass (StaffUser or StudentUser)
 * based on the role selected at program startup, demonstrating
 * polymorphism — the rest of the system works with the abstract User type.
 */
public class UserService {

    private final AtomicInteger idCounter = new AtomicInteger(1);
    private User currentUser = null;

    /**
     * Factory method: creates a StaffUser or StudentUser based on role.
     * This is the polymorphic entry point — callers receive a User reference
     * but the object is of the appropriate concrete subtype.
     */
    public User createSessionUser(String name, Role role) {
        String userId = (role == Role.STAFF ? "S" : "U") + idCounter.getAndIncrement();
        if (role == Role.STAFF) {
            currentUser = new StaffUser(userId, name);
        } else {
            currentUser = new StudentUser(userId, name);
        }
        return currentUser;
    }

    public User getCurrentUser() { return currentUser; }

    public void clearSession() { currentUser = null; }
}

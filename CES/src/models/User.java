package src.models;

import java.util.Objects;

/**
 * Abstract base class for all system users.
 *
 * Inheritance hierarchy (Technical Requirement — inheritance & polymorphism):
 *   User  (abstract)
 *   ├── StaffUser   — can create, update, cancel events; view participants
 *   └── StudentUser — can register/cancel registrations; view own status
 *
 * Polymorphism is used in the UI: the menu shown depends on the runtime type
 * of the User object, via the abstract methods getMenuTitle() and canManageEvents().
 */
public abstract class User {

    private final String userId;
    private final String name;
    private final Role   role;

    protected User(String userId, String name, Role role) {
        this.userId = userId;
        this.name   = name;
        this.role   = role;
    }

    // ─── Getters ─────────────────────────────────────────────────────────────────
    public String getUserId() { return userId; }
    public String getName()   { return name; }
    public Role   getRole()   { return role; }

    // ─── Polymorphic methods (overridden by each subclass) ───────────────────────

    /** One-line label shown in the menu header. */
    public abstract String getMenuTitle();

    /** True if this user type is allowed to create/update/cancel events. */
    public abstract boolean canManageEvents();

    /** Description of what this user type is allowed to do. */
    public abstract String getPermissionSummary();

    // ─── Convenience role checks ──────────────────────────────────────────────────
    public boolean isStaff()   { return role == Role.STAFF; }
    public boolean isStudent() { return role == Role.STUDENT; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return Objects.equals(userId, ((User) o).userId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId); }

    @Override
    public String toString() {
        return String.format("User[id=%s, name=%s, role=%s]", userId, name, role);
    }
}

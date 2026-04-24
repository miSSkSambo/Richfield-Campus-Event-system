package src.models;

/**
 * Concrete User subclass for Staff.
 *
 * Staff users can:
 *   - Create, update, and cancel events
 *   - View all registered participants and waitlists
 *
 * Overrides abstract methods from User to provide staff-specific behaviour.
 */
public class StaffUser extends User {

    public StaffUser(String userId, String name) {
        super(userId, name, Role.STAFF);
    }

    @Override
    public String getMenuTitle() {
        return "STAFF MENU  —  " + getName();
    }

    @Override
    public boolean canManageEvents() {
        return true;   // Staff CAN create, update, cancel events
    }

    @Override
    public String getPermissionSummary() {
        return "Staff: create, update, cancel events; view all participants and waitlists.";
    }
}

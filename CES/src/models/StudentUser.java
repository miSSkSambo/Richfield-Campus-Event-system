package src.models;

/**
 * Concrete User subclass for Students.
 *
 * Students can:
 *   - View available events and search
 *   - Register for events (auto-waitlisted if full)
 *   - Cancel their own registration
 *   - View their registration status (Registered or Waitlisted)
 *
 * Overrides abstract methods from User to provide student-specific behaviour.
 */
public class StudentUser extends User {

    public StudentUser(String userId, String name) {
        super(userId, name, Role.STUDENT);
    }

    @Override
    public String getMenuTitle() {
        return "STUDENT MENU  —  " + getName();
    }

    @Override
    public boolean canManageEvents() {
        return false;  // Students CANNOT create, update, or cancel events
    }

    @Override
    public String getPermissionSummary() {
        return "Student: view events, register/cancel registration, view own status.";
    }
}

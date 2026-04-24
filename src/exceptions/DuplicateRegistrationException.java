package src.exceptions;


/**
 * Thrown when a student tries to register for an event they are
 * already confirmed for or already on the waitlist for.
 */
public class DuplicateRegistrationException extends Exception {
    public DuplicateRegistrationException(String message) {
        super(message);
    }
}

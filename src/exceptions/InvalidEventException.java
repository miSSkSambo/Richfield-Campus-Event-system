package src.exceptions;

/**
 * Thrown when event data fails validation.
 * Examples: empty name, past date, non-positive capacity.
 */
public class InvalidEventException extends Exception {
    public InvalidEventException(String message) {
        super(message);
    }
}

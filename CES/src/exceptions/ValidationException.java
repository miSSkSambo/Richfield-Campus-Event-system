package src.exceptions;

/**
 * Thrown when user input fails type or format validation
 * (e.g. non-numeric ID, wrong date format, blank required field).
 */
public class ValidationException extends Exception {
    private final String fieldName;

    public ValidationException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() { return fieldName; }
}

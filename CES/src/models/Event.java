package src.models;

import src.exceptions.InvalidEventException;
import src.exceptions.ValidationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Represents a campus event.
 *
 * Spec 2.2 fields:
 *   eventId         – unique integer (auto-assigned)
 *   eventName
 *   eventDate       – dd/MM/yyyy
 *   eventTime       – HH:mm
 *   location
 *   maxParticipants
 *
 * Two distinct states (spec 2.2):
 *   registeredParticipants – ArrayList of confirmed user IDs
 *   waitlist               – LinkedList (Queue/FIFO) of overflow user IDs
 *
 * Spec 2.4 — Validation is centralised in the static Validator inner class.
 */
public class Event {

    // ─── Date/time formatters (spec: dd/MM/yyyy and HH:mm) ───────────────────────
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // ─── Fields ──────────────────────────────────────────────────────────────────
    private final int    eventId;
    private String       eventName;
    private LocalDate    eventDate;
    private LocalTime    eventTime;
    private String       location;
    private int          maxParticipants;
    private EventStatus  status;
    private final String createdByStaff;

    // ─── State 1: Registered participant list (ArrayList — spec 2.2) ─────────────
    private final List<String>  registeredParticipants;

    // ─── State 2: Waitlist (LinkedList Queue FIFO — spec 2.2) ────────────────────
    private final Queue<String> waitlist;

    // ─── Constructor ─────────────────────────────────────────────────────────────
    public Event(int eventId, String eventName, LocalDate eventDate, LocalTime eventTime,
                 String location, int maxParticipants, String createdByStaff) {
        this.eventId             = eventId;
        this.eventName           = eventName;
        this.eventDate           = eventDate;
        this.eventTime           = eventTime;
        this.location            = location;
        this.maxParticipants     = maxParticipants;
        this.createdByStaff      = createdByStaff;
        this.status              = EventStatus.UPCOMING;
        this.registeredParticipants = new ArrayList<>();
        this.waitlist               = new LinkedList<>();
    }

    // ─── Getters ─────────────────────────────────────────────────────────────────
    public int         getEventId()          { return eventId; }
    public String      getEventName()        { return eventName; }
    public LocalDate   getEventDate()        { return eventDate; }
    public LocalTime   getEventTime()        { return eventTime; }
    public String      getLocation()         { return location; }
    public int         getMaxParticipants()  { return maxParticipants; }
    public EventStatus getStatus()           { return status; }
    public String      getCreatedByStaff()   { return createdByStaff; }

    public String getFormattedDate() { return eventDate.format(DATE_FORMAT); }
    public String getFormattedTime() { return eventTime.format(TIME_FORMAT); }

    // ─── Setters (spec 2.2: update name, time, location) ────────────────────────
    public void setEventName(String n)      { this.eventName = n; }
    public void setEventDate(LocalDate d)   { this.eventDate = d; }
    public void setEventTime(LocalTime t)   { this.eventTime = t; }
    public void setLocation(String l)       { this.location = l; }
    public void setMaxParticipants(int m)   { this.maxParticipants = m; }
    public void setStatus(EventStatus s)    { this.status = s; }

    // ─── State 1: Registered list ─────────────────────────────────────────────────
    public List<String> getRegisteredParticipants() { return new ArrayList<>(registeredParticipants); }
    public int  getRegisteredCount()                { return registeredParticipants.size(); }
    public int  getAvailableSlots()                 { return Math.max(0, maxParticipants - registeredParticipants.size()); }
    public boolean isFull()                         { return registeredParticipants.size() >= maxParticipants; }
    public boolean isUserRegistered(String userId)  { return registeredParticipants.contains(userId); }

    // ─── State 2: Waitlist ────────────────────────────────────────────────────────
    public List<String> getWaitlistParticipants()   { return new ArrayList<>(waitlist); }
    public int  getWaitlistCount()                  { return waitlist.size(); }
    public boolean isUserOnWaitlist(String userId)  { return waitlist.contains(userId); }

    // ─── Registration logic ───────────────────────────────────────────────────────

    /**
     * Attempt to register a user. Fills the registered list first;
     * overflows automatically to the waitlist when the event is full.
     */
    public RegistrationResult registerUser(String userId) {
        if (status == EventStatus.CANCELLED) return RegistrationResult.EVENT_CANCELLED;
        if (status == EventStatus.COMPLETED) return RegistrationResult.EVENT_COMPLETED;
        if (isUserRegistered(userId))        return RegistrationResult.ALREADY_REGISTERED;
        if (isUserOnWaitlist(userId))        return RegistrationResult.ALREADY_ON_WAITLIST;

        if (!isFull()) {
            registeredParticipants.add(userId);
            return RegistrationResult.REGISTERED;
        } else {
            waitlist.offer(userId);
            return RegistrationResult.ADDED_TO_WAITLIST;
        }
    }

    /**
     * Cancel a confirmed registration or waitlist entry.
     * On confirmed cancellation, the first waitlisted user is auto-promoted (FIFO).
     *
     * @return the userId promoted from the waitlist, or null if none
     */
    public String cancelRegistration(String userId) {
        boolean wasRegistered = registeredParticipants.remove(userId);
        if (!wasRegistered) {
            waitlist.remove(userId);
            return null;
        }
        if (!waitlist.isEmpty()) {
            String promoted = waitlist.poll();
            registeredParticipants.add(promoted);
            return promoted;
        }
        return null;
    }

    // ─── Persistence-restore methods (bypass business-rule checks) ────────────────

    /** Called by PersistenceService to restore a saved confirmed registration. */
    public void loadRegisteredEntry(String userId) {
        if (!registeredParticipants.contains(userId)) {
            registeredParticipants.add(userId);
        }
    }

    /** Called by PersistenceService to restore a saved waitlist entry (order preserved). */
    public void loadWaitlistEntry(String userId) {
        if (!waitlist.contains(userId)) {
            waitlist.offer(userId);
        }
    }

    // ─── Equality ─────────────────────────────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;
        return eventId == ((Event) o).eventId;
    }

    @Override
    public int hashCode() { return Objects.hash(eventId); }

    @Override
    public String toString() {
        return String.format("Event[id=%d, name=%s, date=%s, status=%s]",
                eventId, eventName, getFormattedDate(), status);
    }

    // ─── Inner enum: registration outcomes ───────────────────────────────────────
    public enum RegistrationResult {
        REGISTERED          ("Successfully registered for the event."),
        ADDED_TO_WAITLIST   ("Event is full — you have been added to the waitlist."),
        ALREADY_REGISTERED  ("You are already registered for this event."),
        ALREADY_ON_WAITLIST ("You are already on the waitlist for this event."),
        EVENT_CANCELLED     ("This event has been cancelled and is not accepting registrations."),
        EVENT_COMPLETED     ("This event has already taken place.");

        private final String message;
        RegistrationResult(String msg) { this.message = msg; }
        public String getMessage()     { return message; }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // INNER CLASS: Validator  (spec 2.4 — centralised validation)
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Static helper that validates all event-related input before it reaches
     * the Event constructor or update methods.
     *
     * Using an inner class keeps validation tightly coupled to the entity it
     * validates while keeping service and UI layers clean.
     */
    public static class Validator {

        private Validator() { /* utility class — no instantiation */ }

        /**
         * Validate all fields required to create or update an event.
         *
         * @throws InvalidEventException if any business rule is violated
         * @throws ValidationException   if any input format is wrong
         */
        public static void validateEventFields(String eventName, LocalDate eventDate,
                                               LocalTime eventTime, String location,
                                               int maxParticipants)
                throws InvalidEventException, ValidationException {

            // Non-empty text fields (spec 2.4)
            if (eventName == null || eventName.isBlank()) {
                throw new ValidationException("Event Name", "Event name cannot be empty.");
            }
            if (eventName.length() > 100) {
                throw new ValidationException("Event Name", "Event name must be 100 characters or fewer.");
            }
            if (location == null || location.isBlank()) {
                throw new ValidationException("Location", "Location cannot be empty.");
            }

            // Date must not be in the past (spec: meaningful business rule)
            if (eventDate == null) {
                throw new ValidationException("Event Date", "Event date is required.");
            }
            if (eventDate.isBefore(LocalDate.now())) {
                throw new InvalidEventException(
                        "Event date " + eventDate.format(DATE_FORMAT) +
                        " is in the past. Please choose a future date.");
            }

            // Time is required
            if (eventTime == null) {
                throw new ValidationException("Event Time", "Event time is required.");
            }

            // Positive max participants (spec 2.4)
            if (maxParticipants <= 0) {
                throw new InvalidEventException(
                        "Maximum participants must be a positive number (got " + maxParticipants + ").");
            }
            if (maxParticipants > 100_000) {
                throw new InvalidEventException(
                        "Maximum participants cannot exceed 100,000.");
            }
        }

        /**
         * Validate that an Event ID string is a valid positive integer.
         *
         * @param idString raw string entered by the user
         * @return the parsed integer ID
         * @throws ValidationException if the string is not a valid positive integer
         */
        public static int validateEventId(String idString) throws ValidationException {
            if (idString == null || idString.isBlank()) {
                throw new ValidationException("Event ID", "Event ID cannot be empty.");
            }
            try {
                int id = Integer.parseInt(idString.trim());
                if (id <= 0) {
                    throw new ValidationException("Event ID",
                            "Event ID must be a positive number (got " + id + ").");
                }
                return id;
            } catch (NumberFormatException e) {
                throw new ValidationException("Event ID",
                        "\"" + idString.trim() + "\" is not a valid number. " +
                        "Event ID must be a whole number.");
            }
        }

        /**
         * Validate that a max-participants string is a positive integer.
         */
        public static int validateMaxParticipants(String input) throws ValidationException {
            if (input == null || input.isBlank()) {
                throw new ValidationException("Max Participants", "This field cannot be empty.");
            }
            try {
                int val = Integer.parseInt(input.trim());
                if (val <= 0) {
                    throw new ValidationException("Max Participants",
                            "Maximum participants must be greater than zero.");
                }
                return val;
            } catch (NumberFormatException e) {
                throw new ValidationException("Max Participants",
                        "\"" + input.trim() + "\" is not a valid number.");
            }
        }

        /**
         * Validate a date string in dd/MM/yyyy format.
         */
        public static LocalDate validateDate(String input) throws ValidationException {
            if (input == null || input.isBlank()) {
                throw new ValidationException("Event Date", "Date cannot be empty.");
            }
            try {
                return LocalDate.parse(input.trim(), DATE_FORMAT);
            } catch (Exception e) {
                throw new ValidationException("Event Date",
                        "\"" + input.trim() + "\" is not a valid date. Use dd/MM/yyyy (e.g. 25/12/2025).");
            }
        }

        /**
         * Validate a time string in HH:mm format.
         */
        public static LocalTime validateTime(String input) throws ValidationException {
            if (input == null || input.isBlank()) {
                throw new ValidationException("Event Time", "Time cannot be empty.");
            }
            try {
                return LocalTime.parse(input.trim(), TIME_FORMAT);
            } catch (Exception e) {
                throw new ValidationException("Event Time",
                        "\"" + input.trim() + "\" is not a valid time. Use HH:mm (e.g. 14:30).");
            }
        }

        /**
         * Validate that a text field is not blank.
         */
        public static String validateNonBlank(String input, String fieldName)
                throws ValidationException {
            if (input == null || input.isBlank()) {
                throw new ValidationException(fieldName, fieldName + " cannot be empty.");
            }
            return input.trim();
        }
    }
}

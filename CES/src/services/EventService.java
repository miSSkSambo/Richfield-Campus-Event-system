package src.services;

import src.exceptions.*;
import src.models.*;
import src.services.NotificationService;
import src.services.PersistenceService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Core business logic for event management.
 *
 * Spec 2.2 — staff capabilities: create, update, cancel, view (with sort).
 * Spec 2.4 — search by name/date; validation; error handling; persistence.
 *
 * Data structures used (spec Technical Requirements):
 *   LinkedHashMap<Integer, Event>  – preserves insertion order, O(1) lookup by ID
 *   ArrayList                      – registered participant list inside Event
 *   LinkedList (Queue)             – waitlist inside Event (FIFO)
 */
public class EventService {

    // ─── Data structures ─────────────────────────────────────────────────────────
    private final Map<Integer, Event> eventsById = new LinkedHashMap<>();
    private final AtomicInteger idCounter        = new AtomicInteger(1);

    // ─── Collaborators ────────────────────────────────────────────────────────────
    private final NotificationService notificationService;
    private final PersistenceService  persistenceService;

    // ─── Constructor: load saved data on startup ───────────────────────────────────
    public EventService(NotificationService notificationService,
                        PersistenceService persistenceService) {
        this.notificationService = notificationService;
        this.persistenceService  = persistenceService;
        loadFromDisk();
    }

    // ─── Persistence: load ────────────────────────────────────────────────────────
    private void loadFromDisk() {
        Map<Integer, Event> loaded = persistenceService.loadEvents();

        if (loaded.isEmpty()) {
            // First run — seed sample data so the system is immediately usable
            seedSampleEvents();
        } else {
            eventsById.putAll(loaded);
            persistenceService.loadRegistrations(eventsById);
            persistenceService.loadWaitlists(eventsById);
            // Resume ID counter from highest saved ID
            int maxId = persistenceService.getMaxSavedEventId(eventsById);
            idCounter.set(maxId + 1);
            System.out.println("  [System] Loaded " + eventsById.size() +
                    " event(s) from saved data.");
        }
    }

    /** Save everything to disk. Call after every mutating operation. */
    private void persist() {
        persistenceService.saveAll(new ArrayList<>(eventsById.values()));
    }

    // ─── Seed data (first run only) ───────────────────────────────────────────────
    private void seedSampleEvents() {
        LocalDate today = LocalDate.now();
        try {
            createEvent("AI & Machine Learning Bootcamp",     today.plusDays(5),  LocalTime.of(10,  0), "Computer Lab B",      60, "System");
            createEvent("Entrepreneurship Pitch Night",       today.plusDays(7),  LocalTime.of(17,  0), "Innovation Hub",      80, "System");
            createEvent("Cybersecurity Awareness Day",        today.plusDays(10), LocalTime.of(9, 0),   "Lecture Hall C",      120, "System");
            createEvent("Mobile App Development Workshop",    today.plusDays(12), LocalTime.of(13, 0),  "IT Building Room 2",  40, "System");
            createEvent("Cultural Food Festival",             today.plusDays(15), LocalTime.of(12, 0),  "Campus Courtyard",    200, "System");
        } catch (InvalidEventException | ValidationException e) {
            System.err.println("[Seed] Unexpected error seeding events: " + e.getMessage());
        }
    } 

    // ═════════════════════════════════════════════════════════════════════════════
    // CREATE  (spec 2.2)
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Create a new event after full validation.
     *
     * @throws InvalidEventException if business rules are violated (past date, etc.)
     * @throws ValidationException   if input format is invalid
     */
    public Event createEvent(String eventName, LocalDate eventDate, LocalTime eventTime,
                             String location, int maxParticipants, String staffName)
            throws InvalidEventException, ValidationException {

        // Validate all fields before touching any state
        Event.Validator.validateEventFields(eventName, eventDate, eventTime,
                location, maxParticipants);

        int id = idCounter.getAndIncrement();
        Event event = new Event(id, eventName, eventDate, eventTime,
                location, maxParticipants, staffName);
        eventsById.put(id, event);
        persist();
        return event;
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // READ & SEARCH  (spec 2.4)
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Look up an event by its integer ID.
     *
     * @throws EventNotFoundException if the ID does not exist
     */
    public Event getEventById(int eventId) throws EventNotFoundException {
        Event event = eventsById.get(eventId);
        if (event == null) throw new EventNotFoundException(eventId);
        return event;
    }

    public List<Event> getAllEvents() {
        return new ArrayList<>(eventsById.values());
    }

    public List<Event> getUpcomingEvents() {
        return eventsById.values().stream()
                .filter(e -> e.getStatus() == EventStatus.UPCOMING)
                .collect(Collectors.toList());
    }

    // ─── Search by Event Name (spec 2.4: partial or full match) ─────────────────

    /**
     * Search events where the event name contains the keyword (case-insensitive).
     * Partial match is supported — e.g. "sci" matches "Annual Science Fair".
     *
     * @param keyword partial or full event name to search for
     * @return list of matching events, sorted by date
     * @throws ValidationException if keyword is blank
     */
    public List<Event> searchByName(String keyword) throws ValidationException {
        Event.Validator.validateNonBlank(keyword, "Search keyword");
        String lower = keyword.toLowerCase().trim();
        return eventsById.values().stream()
                .filter(e -> e.getEventName().toLowerCase().contains(lower))
                .sorted(Comparator.comparing(Event::getEventDate)
                        .thenComparing(Event::getEventTime))
                .collect(Collectors.toList());
    }

    // ─── Search by Event Date (spec 2.4) ─────────────────────────────────────────

    /**
     * Find all events on a specific date.
     *
     * @param date the exact date to search for
     * @return list of events on that date, sorted by time
     */
    public List<Event> searchByDate(LocalDate date) {
        return eventsById.values().stream()
                .filter(e -> e.getEventDate().equals(date))
                .sorted(Comparator.comparing(Event::getEventTime))
                .collect(Collectors.toList());
    }

    // ─── Sorting (spec 2.2) ───────────────────────────────────────────────────────

    public List<Event> getAllEventsSortedByName() {
        return eventsById.values().stream()
                .sorted(Comparator.comparing(e -> e.getEventName().toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Event> getAllEventsSortedByDate() {
        return eventsById.values().stream()
                .sorted(Comparator.comparing(Event::getEventDate)
                        .thenComparing(Event::getEventTime))
                .collect(Collectors.toList());
    }

    // ─── Per-user lookups ─────────────────────────────────────────────────────────

    public List<Event> getRegisteredEventsForUser(String userId) {
        return eventsById.values().stream()
                .filter(e -> e.isUserRegistered(userId))
                .sorted(Comparator.comparing(Event::getEventDate))
                .collect(Collectors.toList());
    }

    public List<Event> getWaitlistedEventsForUser(String userId) {
        return eventsById.values().stream()
                .filter(e -> e.isUserOnWaitlist(userId))
                .sorted(Comparator.comparing(Event::getEventDate))
                .collect(Collectors.toList());
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // UPDATE  (spec 2.2: name, time, or location; also date and maxParticipants)
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Update the mutable fields of an event.
     * Pass null to leave any field unchanged.
     *
     * @throws EventNotFoundException if the event ID does not exist
     * @throws InvalidEventException  if the new values fail validation
     * @throws ValidationException    if input format is wrong
     */
    public void updateEvent(int eventId,
                            String newName,
                            LocalDate newDate,
                            LocalTime newTime,
                            String newLocation,
                            Integer newMaxParticipants)
            throws EventNotFoundException, InvalidEventException, ValidationException {

        Event event = getEventById(eventId);   // throws if not found

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new InvalidEventException("Cannot update a cancelled event.");
        }

        // Determine resolved values (keep existing if null passed)
        String    resolvedName = (newName     != null && !newName.isBlank())     ? newName     : event.getEventName();
        LocalDate resolvedDate = (newDate     != null)                           ? newDate     : event.getEventDate();
        LocalTime resolvedTime = (newTime     != null)                           ? newTime     : event.getEventTime();
        String    resolvedLoc  = (newLocation != null && !newLocation.isBlank()) ? newLocation : event.getLocation();
        int       resolvedMax  = (newMaxParticipants != null)                    ? newMaxParticipants : event.getMaxParticipants();

        // Validate resolved values together
        Event.Validator.validateEventFields(resolvedName, resolvedDate, resolvedTime,
                resolvedLoc, resolvedMax);

        // Apply changes and track what changed for notification
        StringBuilder changes = new StringBuilder();
        if (!resolvedName.equals(event.getEventName())) {
            changes.append("Name: \"").append(resolvedName).append("\". ");
            event.setEventName(resolvedName);
        }
        if (!resolvedDate.equals(event.getEventDate())) {
            changes.append("Date: ").append(resolvedDate.format(Event.DATE_FORMAT)).append(". ");
            event.setEventDate(resolvedDate);
        }
        if (!resolvedTime.equals(event.getEventTime())) {
            changes.append("Time: ").append(resolvedTime.format(Event.TIME_FORMAT)).append(". ");
            event.setEventTime(resolvedTime);
        }
        if (!resolvedLoc.equals(event.getLocation())) {
            changes.append("Location: ").append(resolvedLoc).append(". ");
            event.setLocation(resolvedLoc);
        }
        if (resolvedMax != event.getMaxParticipants()) {
            changes.append("Max participants: ").append(resolvedMax).append(". ");
            event.setMaxParticipants(resolvedMax);
        }

        if (changes.length() > 0 && !event.getRegisteredParticipants().isEmpty()) {
            notificationService.notifyEventUpdated(event.getRegisteredParticipants(),
                    event.getEventName(), changes.toString().trim());
        }
        persist();
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // CANCEL  (spec 2.2)
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Cancel an event and notify all registered and waitlisted users.
     *
     * @throws EventNotFoundException if the event ID does not exist
     * @throws InvalidEventException  if the event is already cancelled
     */
    public void cancelEvent(int eventId)
            throws EventNotFoundException, InvalidEventException {

        Event event = getEventById(eventId);

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new InvalidEventException(
                    "Event \"" + event.getEventName() + "\" is already cancelled.");
        }

        List<String> affected = new ArrayList<>(event.getRegisteredParticipants());
        affected.addAll(event.getWaitlistParticipants());

        event.setStatus(EventStatus.CANCELLED);

        if (!affected.isEmpty()) {
            notificationService.notifyEventCancelled(affected, event.getEventName());
        }
        persist();
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // REGISTRATION  (students)
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Register a student for an event.
     * Auto-adds to waitlist if full.
     *
     * @throws EventNotFoundException        if the event ID is invalid
     * @throws DuplicateRegistrationException if already registered or waitlisted
     * @throws InvalidEventException          if the event is not open
     */
    public Event.RegistrationResult registerUserForEvent(String userId, int eventId)
            throws EventNotFoundException, DuplicateRegistrationException, InvalidEventException {

        Event event = getEventById(eventId);

        // Check status before attempting
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new InvalidEventException(
                    "\"" + event.getEventName() + "\" has been cancelled.");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new InvalidEventException(
                    "\"" + event.getEventName() + "\" has already taken place.");
        }

        Event.RegistrationResult result = event.registerUser(userId);

        switch (result) {
            case ALREADY_REGISTERED ->
                    throw new DuplicateRegistrationException(
                            "You are already registered for \"" + event.getEventName() + "\".");
            case ALREADY_ON_WAITLIST ->
                    throw new DuplicateRegistrationException(
                            "You are already on the waitlist for \"" + event.getEventName() + "\".");
            case REGISTERED ->
                    notificationService.notifyRegistrationConfirmed(userId, event.getEventName());
            case ADDED_TO_WAITLIST -> {
                int pos = event.getWaitlistCount();
                notificationService.notifyAddedToWaitlist(userId, event.getEventName(), pos);
            }
            default -> { /* no-op */ }
        }

        persist();
        return result;
    }

    /**
     * Cancel a student's confirmed or waitlisted registration.
     * Auto-promotes the next waitlisted user if a confirmed spot is freed.
     *
     * @throws EventNotFoundException if the event ID is invalid
     * @throws InvalidEventException  if the user is not registered or waitlisted
     */
    public void cancelRegistration(String userId, int eventId)
            throws EventNotFoundException, InvalidEventException {

        Event event = getEventById(eventId);

        if (!event.isUserRegistered(userId) && !event.isUserOnWaitlist(userId)) {
            throw new InvalidEventException(
                    "You are not registered or waitlisted for \"" + event.getEventName() + "\".");
        }

        String promoted = event.cancelRegistration(userId);
        notificationService.notifyRegistrationCancelled(userId, event.getEventName());

        if (promoted != null) {
            notificationService.notifyPromotedFromWaitlist(promoted, event.getEventName());
        }
        persist();
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // STATUS REFRESH
    // ═════════════════════════════════════════════════════════════════════════════

    public void refreshEventStatuses() {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();
        boolean changed = false;

        for (Event event : eventsById.values()) {
            if (event.getStatus() == EventStatus.CANCELLED
                    || event.getStatus() == EventStatus.COMPLETED) continue;

            if (event.getEventDate().isBefore(today)) {
                event.setStatus(EventStatus.COMPLETED);
                changed = true;
            } else if (event.getEventDate().equals(today)
                    && event.getEventTime().isBefore(now)) {
                event.setStatus(EventStatus.ONGOING);
                changed = true;
            }
        }
        if (changed) persist();
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // STATISTICS
    // ═════════════════════════════════════════════════════════════════════════════

    public int getTotalEventCount()                      { return eventsById.size(); }
    public int getEventCountByStatus(EventStatus status) {
        return (int) eventsById.values().stream()
                .filter(e -> e.getStatus() == status).count();
    }
    public int getTotalRegistrations() {
        return eventsById.values().stream().mapToInt(Event::getRegisteredCount).sum();
    }
    public int getTotalWaitlisted() {
        return eventsById.values().stream().mapToInt(Event::getWaitlistCount).sum();
    }
    public Optional<Event> getMostPopularEvent() {
        return eventsById.values().stream()
                .max(Comparator.comparingInt(Event::getRegisteredCount));
    }
}
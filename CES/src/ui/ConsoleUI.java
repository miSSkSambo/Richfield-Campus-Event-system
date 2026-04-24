package src.ui;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;
import src.exceptions.*;
import src.models.Role;
import src.models.*;
import src.services.*;

/**
 * Console UI — Richfield Campus Event Management System.
 *
 * Implements spec sections 2.1, 2.2, 2.4:
 *   - Role selection at startup (Staff / Student)
 *   - Polymorphic dispatch: menu shown depends on runtime User subtype
 *   - Search by Event Name (partial/full) and by Event Date
 *   - Full validation with informative error messages
 *   - Graceful error handling (no unhandled exceptions reach the user)
 *   - Data persisted automatically after every change
 */
public class ConsoleUI {

    // ─── ANSI colours ─────────────────────────────────────────────────────────────
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String CYAN   = "\u001B[36m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED    = "\u001B[31m";
    private static final String BLUE   = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";

    // ─── Services ─────────────────────────────────────────────────────────────────
    private final UserService         userService;
    private final NotificationService notificationService;
    private final PersistenceService  persistenceService;
    private final EventService        eventService;
    private final Scanner             scanner;

    // ─── Polymorphic User reference (StaffUser or StudentUser at runtime) ─────────
    private User currentUser = null;

    public ConsoleUI() {
        this.userService         = new UserService();
        this.notificationService = new NotificationService();
        this.persistenceService  = new PersistenceService();
        this.eventService        = new EventService(notificationService, persistenceService);
        this.scanner             = new Scanner(System.in);
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // START
    // ═════════════════════════════════════════════════════════════════════════════

    public void start() {
        printBanner();
        selectRole();

        boolean running = true;
        while (running) {
            eventService.refreshEventStatuses();
            // Polymorphic dispatch: canManageEvents() is overridden in each subclass
            running = currentUser.canManageEvents() ? showStaffMenu() : showStudentMenu();
        }
        System.out.println("\n" + CYAN + "Thank you for using Richfield CEMS. Goodbye!" + RESET);
    }

    // ─── Role selection ───────────────────────────────────────────────────────────
    private void selectRole() {
        System.out.println(BOLD + "  Select your role to continue:" + RESET);
        System.out.println("  1. Staff");
        System.out.println("  2. Student");
        printLine();

        Role role = null;
        while (role == null) {
            switch (readInt("Enter 1 or 2")) {
                case 1 -> role = Role.STAFF;
                case 2 -> role = Role.STUDENT;
                default -> printError("Please enter 1 (Staff) or 2 (Student).");
            }
        }

        String name = "";
        while (name.isBlank()) {
            name = readLine("Enter your name");
            if (name.isBlank()) printError("Name cannot be empty.");
        }

        // Factory method returns StaffUser or StudentUser — polymorphism
        currentUser = userService.createSessionUser(name, role);

        System.out.println();
        printSuccess("Welcome, " + currentUser.getName() + "!  Signed in as "
                + BOLD + role.getDisplayName() + RESET + GREEN + ".");
        System.out.println(YELLOW + "  ℹ  " + currentUser.getPermissionSummary() + RESET);
        System.out.println();
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // STAFF MENU
    // ═════════════════════════════════════════════════════════════════════════════

    private boolean showStaffMenu() {
        // getMenuTitle() is polymorphic — defined in StaffUser
        printHeader(currentUser.getMenuTitle());
        System.out.println("  1. View All Events");
        System.out.println("  2. Search Events");
        System.out.println("  3. Create New Event");
        System.out.println("  4. Update Event Details");
        System.out.println("  5. Cancel an Event");
        System.out.println("  6. View Participants & Waitlist");
        System.out.println("  0. Exit");
        printLine();

        switch (readInt("Select option")) {
            case 1 -> staffViewAllEvents();
            case 2 -> searchEvents();
            case 3 -> staffCreateEvent();
            case 4 -> staffUpdateEvent();
            case 5 -> staffCancelEvent();
            case 6 -> staffViewParticipants();
            case 0 -> { return false; }
            default -> printError("Please choose 0 – 6.");
        }
        return true;
    }

    // ── View all events ───────────────────────────────────────────────────────────
    private void staffViewAllEvents() {
        List<Event> events = sortedEvents(eventService.getAllEvents());
        printEventTable(events, "ALL EVENTS");
        pause();
    }

    // ── Create event ──────────────────────────────────────────────────────────────
    private void staffCreateEvent() {
        printSubHeader("CREATE NEW EVENT");
        try {
            String    name  = readValidatedNonBlank("Event Name");
            LocalDate date  = readValidatedDate("Event Date (dd/MM/yyyy)");
            LocalTime time  = readValidatedTime("Event Time (HH:mm)");
            String    loc   = readValidatedNonBlank("Location");
            int       maxP  = readValidatedPositiveInt("Maximum Participants");

            Event created = eventService.createEvent(
                    name, date, time, loc, maxP, currentUser.getName());

            printSuccess("Event created and saved!");
            printLine();
            System.out.printf("  %-22s %d%n",  "Event ID:",          created.getEventId());
            System.out.printf("  %-22s %s%n",  "Event Name:",        created.getEventName());
            System.out.printf("  %-22s %s%n",  "Date:",              created.getFormattedDate());
            System.out.printf("  %-22s %s%n",  "Time:",              created.getFormattedTime());
            System.out.printf("  %-22s %s%n",  "Location:",          created.getLocation());
            System.out.printf("  %-22s %d%n",  "Max Participants:",  created.getMaxParticipants());
            printLine();

        } catch (InvalidEventException | ValidationException e) {
            printError(e.getMessage());
        }
        pause();
    }

    // ── Update event ──────────────────────────────────────────────────────────────
    private void staffUpdateEvent() {
        printSubHeader("UPDATE EVENT DETAILS");
        List<Event> all = sortedEvents(eventService.getAllEvents());
        printEventTable(all, "ALL EVENTS");

        try {
            int   eventId = readValidatedEventId();
            Event event   = eventService.getEventById(eventId);

            printEventDetail(event);
            System.out.println(YELLOW + "  Press ENTER to keep the current value." + RESET + "\n");

            // ── name ──────────────────────────────────────────────────────────────
            System.out.print("  " + CYAN + "Event Name     [" + event.getEventName() + "]: " + RESET);
            String nameIn = scanner.nextLine().trim();
            String newName = nameIn.isEmpty() ? null : nameIn;

            // ── date ──────────────────────────────────────────────────────────────
            System.out.print("  " + CYAN + "Event Date     [" + event.getFormattedDate() + "]: " + RESET);
            String dateIn = scanner.nextLine().trim();
            LocalDate newDate = null;
            if (!dateIn.isEmpty()) {
                newDate = Event.Validator.validateDate(dateIn);
            }

            // ── time ──────────────────────────────────────────────────────────────
            System.out.print("  " + CYAN + "Event Time     [" + event.getFormattedTime() + "]: " + RESET);
            String timeIn = scanner.nextLine().trim();
            LocalTime newTime = null;
            if (!timeIn.isEmpty()) {
                newTime = Event.Validator.validateTime(timeIn);
            }

            // ── location ──────────────────────────────────────────────────────────
            System.out.print("  " + CYAN + "Location       [" + event.getLocation() + "]: " + RESET);
            String locIn = scanner.nextLine().trim();
            String newLoc = locIn.isEmpty() ? null : locIn;

            // ── max participants ───────────────────────────────────────────────────
            System.out.print("  " + CYAN + "Max Participants [" + event.getMaxParticipants() + "]: " + RESET);
            String maxIn = scanner.nextLine().trim();
            Integer newMax = null;
            if (!maxIn.isEmpty()) {
                newMax = Event.Validator.validateMaxParticipants(maxIn);
            }

            eventService.updateEvent(eventId, newName, newDate, newTime, newLoc, newMax);
            printSuccess("Event updated and saved.");

        } catch (EventNotFoundException e) {
            printError(e.getMessage());
        } catch (InvalidEventException | ValidationException e) {
            printError("Update failed: " + e.getMessage());
        }
        pause();
    }

    // ── Cancel event ──────────────────────────────────────────────────────────────
    private void staffCancelEvent() {
        printSubHeader("CANCEL AN EVENT");
        List<Event> all = sortedEvents(eventService.getAllEvents());
        printEventTable(all, "ALL EVENTS");

        try {
            int   eventId = readValidatedEventId();
            Event event   = eventService.getEventById(eventId);

            int total = event.getRegisteredCount() + event.getWaitlistCount();
            printWarning("You are about to cancel: \"" + event.getEventName() + "\"");
            System.out.printf("  Date:     %s at %s%n", event.getFormattedDate(), event.getFormattedTime());
            System.out.printf("  Affected: %d participant(s) + %d on waitlist%n",
                    event.getRegisteredCount(), event.getWaitlistCount());
            System.out.print("\n  Type " + BOLD + "CONFIRM" + RESET + " to proceed, or press ENTER to abort: ");
            String confirm = scanner.nextLine().trim();

            if ("CONFIRM".equalsIgnoreCase(confirm)) {
                eventService.cancelEvent(eventId);
                printSuccess("Event cancelled and saved. " + total + " user(s) notified.");
            } else {
                System.out.println("  Aborted — no changes made.");
            }

        } catch (EventNotFoundException | InvalidEventException | ValidationException e) {
            printError(e.getMessage());
        }
        pause();
    }

    // ── View participants & waitlist ──────────────────────────────────────────────
    private void staffViewParticipants() {
        printSubHeader("VIEW PARTICIPANTS & WAITLIST");
        List<Event> all = sortedEvents(eventService.getAllEvents());
        printEventTable(all, "ALL EVENTS");

        try {
            int   eventId = readValidatedEventId();
            Event event   = eventService.getEventById(eventId);

            System.out.println();
            System.out.println(BOLD + CYAN + "  ═══ " + event.getEventName() + " ═══" + RESET);
            System.out.printf("  Date: %s  |  Time: %s  |  Location: %s%n",
                    event.getFormattedDate(), event.getFormattedTime(), event.getLocation());
            System.out.printf("  Status: %s%s%s%n",
                    statusColour(event.getStatus()), event.getStatus().getDisplayName(), RESET);
            printLine();

            // State 1: Registered list
            List<String> reg = event.getRegisteredParticipants();
            System.out.printf("%n" + BOLD + GREEN
                    + "  REGISTERED PARTICIPANTS  (%d / %d)%n" + RESET,
                    reg.size(), event.getMaxParticipants());
            if (reg.isEmpty()) {
                System.out.println("  (No confirmed participants yet)");
            } else {
                System.out.printf("  %-5s  %-30s  %s%n", "No.", "User ID", "Status");
                printLine();
                for (int i = 0; i < reg.size(); i++) {
                    System.out.printf("  %-5d  %-30s  %sRegistered%s%n",
                            i + 1, reg.get(i), GREEN, RESET);
                }
            }

            // State 2: Waitlist
            List<String> wait = event.getWaitlistParticipants();
            System.out.printf("%n" + BOLD + YELLOW
                    + "  WAITLIST  (%d)%n" + RESET, wait.size());
            if (wait.isEmpty()) {
                System.out.println("  (Waitlist is empty)");
            } else {
                System.out.printf("  %-5s  %-30s  %s%n", "Pos.", "User ID", "Status");
                printLine();
                for (int i = 0; i < wait.size(); i++) {
                    System.out.printf("  %-5d  %-30s  %sWaitlisted%s%n",
                            i + 1, wait.get(i), YELLOW, RESET);
                }
            }
            printLine();

        } catch (EventNotFoundException | ValidationException e) {
            printError(e.getMessage());
        }
        pause();
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // SEARCH  (spec 2.4 — shared between Staff and Student)
    // ═════════════════════════════════════════════════════════════════════════════

    private void searchEvents() {
        boolean back = false;
        while (!back) {
            printSubHeader("SEARCH EVENTS");
            System.out.println("  1. Search by Event Name  (partial or full match)");
            System.out.println("  2. Search by Event Date  (dd/MM/yyyy)");
            System.out.println("  0. Back");
            printLine();

            switch (readInt("Select option")) {
                case 1 -> searchByName();
                case 2 -> searchByDate();
                case 0 -> back = true;
                default -> printError("Please choose 0 – 2.");
            }
        }
    }

    /** Search by Event Name — partial or full match (spec 2.4). */
    private void searchByName() {
        printSubHeader("SEARCH BY EVENT NAME");
        try {
            String keyword = readLine("Enter event name (or part of it)");
            List<Event> results = eventService.searchByName(keyword);

            if (results.isEmpty()) {
                printWarning("No events found matching \"" + keyword + "\".");
            } else {
                printEventTable(results, "RESULTS FOR: \"" + keyword + "\"");
                // Show full details for each result (spec 2.4)
                System.out.println("\n  Showing full details for all matching events:\n");
                for (Event e : results) printEventDetail(e);
            }
        } catch (ValidationException e) {
            printError(e.getMessage());
        }
        pause();
    }

    /** Search by Event Date (spec 2.4). */
    private void searchByDate() {
        printSubHeader("SEARCH BY EVENT DATE");
        try {
            String    input = readLine("Enter date (dd/MM/yyyy)");
            LocalDate date  = Event.Validator.validateDate(input);
            List<Event> results = eventService.searchByDate(date);

            if (results.isEmpty()) {
                printWarning("No events found on " + date.format(Event.DATE_FORMAT) + ".");
            } else {
                printEventTable(results, "EVENTS ON " + date.format(Event.DATE_FORMAT));
                System.out.println("\n  Full details:\n");
                for (Event e : results) printEventDetail(e);
            }
        } catch (ValidationException e) {
            printError(e.getMessage());
        }
        pause();
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // STUDENT MENU
    // ═════════════════════════════════════════════════════════════════════════════

    private boolean showStudentMenu() {
        int unread = notificationService.getUnreadCount(currentUser.getUserId());
        String badge = unread > 0 ? RED + "  [" + unread + " new]" + RESET : "";

        // getMenuTitle() is polymorphic — defined in StudentUser
        printHeader(currentUser.getMenuTitle() + badge);
        System.out.println("  1. View Available Events");
        System.out.println("  2. Search Events");
        System.out.println("  3. Register for an Event");
        System.out.println("  4. Cancel My Registration");
        System.out.println("  5. View My Registration Status");
        System.out.println("  6. My Notifications");
        System.out.println("  0. Exit");
        printLine();

        switch (readInt("Select option")) {
            case 1 -> studentViewEvents();
            case 2 -> searchEvents();
            case 3 -> studentRegister();
            case 4 -> studentCancel();
            case 5 -> studentViewStatus();
            case 6 -> studentViewNotifications();
            case 0 -> { return false; }
            default -> printError("Please choose 0 – 6.");
        }
        return true;
    }

    // ── View available events ─────────────────────────────────────────────────────
    private void studentViewEvents() {
        List<Event> upcoming = sortedEvents(eventService.getUpcomingEvents());
        printEventTable(upcoming, "UPCOMING EVENTS");
        pause();
    }

    // ── Register ──────────────────────────────────────────────────────────────────
    private void studentRegister() {
        printSubHeader("REGISTER FOR AN EVENT");
        List<Event> upcoming = sortedEvents(eventService.getUpcomingEvents());
        printEventTable(upcoming, "UPCOMING EVENTS");

        try {
            int   eventId = readValidatedEventId();
            Event event   = eventService.getEventById(eventId);
            printEventDetail(event);

            Event.RegistrationResult result =
                    eventService.registerUserForEvent(currentUser.getUserId(), eventId);

            switch (result) {
                case REGISTERED        -> printSuccess("✔  " + result.getMessage());
                case ADDED_TO_WAITLIST -> printWarning("⏳ " + result.getMessage());
                default                -> printWarning(result.getMessage());
            }

        } catch (EventNotFoundException | InvalidEventException | ValidationException e) {
            printError(e.getMessage());
        } catch (DuplicateRegistrationException e) {
            printError("Already registered: " + e.getMessage());
        }
        pause();
    }

    // ── Cancel registration ───────────────────────────────────────────────────────
    private void studentCancel() {
        printSubHeader("CANCEL MY REGISTRATION");

        List<Event> registered = eventService.getRegisteredEventsForUser(currentUser.getUserId());
        List<Event> waitlisted = eventService.getWaitlistedEventsForUser(currentUser.getUserId());

        if (registered.isEmpty() && waitlisted.isEmpty()) {
            printWarning("You have no current registrations to cancel."); pause(); return;
        }

        if (!registered.isEmpty()) {
            System.out.println(GREEN + BOLD + "\n  YOUR CONFIRMED REGISTRATIONS:" + RESET);
            printEventTable(registered, "");
        }
        if (!waitlisted.isEmpty()) {
            System.out.println(YELLOW + BOLD + "\n  YOUR WAITLIST POSITIONS:" + RESET);
            printEventTable(waitlisted, "");
        }

        try {
            int   eventId = readValidatedEventId();
            Event event   = eventService.getEventById(eventId);

            boolean isReg  = event.isUserRegistered(currentUser.getUserId());
            boolean isWait = event.isUserOnWaitlist(currentUser.getUserId());

            if (!isReg && !isWait) {
                printError("You are not registered or waitlisted for that event."); pause(); return;
            }

            String type = isReg ? "registration" : "waitlist position";
            System.out.print("  Confirm cancel your " + type
                    + " for \"" + event.getEventName() + "\"? (y/n): ");

            if ("y".equalsIgnoreCase(scanner.nextLine().trim())) {
                eventService.cancelRegistration(currentUser.getUserId(), eventId);
                printSuccess("Your " + type + " has been cancelled and saved.");
            } else {
                System.out.println("  Aborted — no changes made.");
            }

        } catch (EventNotFoundException | InvalidEventException e) {
            printError(e.getMessage());
        } catch (ValidationException e) {
            printError("Invalid Event ID: " + e.getMessage());
        }
        pause();
    }

    // ── View registration status ──────────────────────────────────────────────────
    private void studentViewStatus() {
        printSubHeader("MY REGISTRATION STATUS");

        List<Event> registered = eventService.getRegisteredEventsForUser(currentUser.getUserId());
        List<Event> waitlisted = eventService.getWaitlistedEventsForUser(currentUser.getUserId());

        if (registered.isEmpty() && waitlisted.isEmpty()) {
            printWarning("You have not registered for any events yet."); pause(); return;
        }

        System.out.printf("%n  %-22s %s%n", "Name:",    currentUser.getName());
        System.out.printf("  %-22s %s%n",   "User ID:", currentUser.getUserId());
        printLine();

        if (!registered.isEmpty()) {
            System.out.println(BOLD + GREEN + "\n  ✔  REGISTERED (Confirmed):" + RESET);
            System.out.printf("  %-6s  %-32s  %-12s  %-7s  %s%n",
                    "ID", "Event Name", "Date", "Time", "Status");
            printLine();
            for (Event e : registered) {
                System.out.printf("  %-6d  %-32s  %-12s  %-7s  %sRegistered%s%n",
                        e.getEventId(), truncate(e.getEventName(), 32),
                        e.getFormattedDate(), e.getFormattedTime(), GREEN, RESET);
            }
        }

        if (!waitlisted.isEmpty()) {
            System.out.println(BOLD + YELLOW + "\n  ⏳ WAITLISTED:" + RESET);
            System.out.printf("  %-6s  %-32s  %-12s  %-7s  %s%n",
                    "ID", "Event Name", "Date", "Time", "Position");
            printLine();
            for (Event e : waitlisted) {
                int pos = e.getWaitlistParticipants().indexOf(currentUser.getUserId()) + 1;
                System.out.printf("  %-6d  %-32s  %-12s  %-7s  %sWaitlist #%d%s%n",
                        e.getEventId(), truncate(e.getEventName(), 32),
                        e.getFormattedDate(), e.getFormattedTime(), YELLOW, pos, RESET);
            }
        }

        printLine();
        System.out.printf("  Confirmed:  %d%n", registered.size());
        System.out.printf("  Waitlisted: %d%n", waitlisted.size());
        pause();
    }

    // ── Notifications ─────────────────────────────────────────────────────────────
    private void studentViewNotifications() {
        printSubHeader("MY NOTIFICATIONS");
        List<Notification> list =
                notificationService.getNotificationsForUser(currentUser.getUserId());

        if (list.isEmpty()) { printWarning("No notifications yet."); pause(); return; }

        for (int i = 0; i < list.size(); i++) {
            Notification n = list.get(i);
            String dot = n.isRead() ? "  " : GREEN + "● " + RESET;
            System.out.printf("%s%d. [%s]  %s%s%s%n",
                    dot, i + 1, n.getFormattedDate(), BOLD, n.getSubject(), RESET);
            System.out.println("     " + n.getMessage() + "\n");
        }
        notificationService.markAllAsRead(currentUser.getUserId());
        printSuccess("All notifications marked as read.");
        pause();
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // SORT HELPER  (spec 2.2)
    // ═════════════════════════════════════════════════════════════════════════════

    private List<Event> sortedEvents(List<Event> events) {
        System.out.println("\n  " + BOLD + "Sort by:" + RESET
                + "  1. Event Name (A→Z)   2. Event Date (earliest first)   3. None");
        switch (readInt("Sort")) {
            case 1 -> {
                events.sort((a, b) -> a.getEventName().compareToIgnoreCase(b.getEventName()));
                System.out.println(CYAN + "  Sorted by Event Name." + RESET);
            }
            case 2 -> {
                events.sort(java.util.Comparator.comparing(Event::getEventDate)
                        .thenComparing(Event::getEventTime));
                System.out.println(CYAN + "  Sorted by Event Date." + RESET);
            }
            default -> { /* no sort */ }
        }
        return events;
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // EVENT TABLE & DETAIL DISPLAY
    // ═════════════════════════════════════════════════════════════════════════════

    private void printEventTable(List<Event> events, String heading) {
        if (heading != null && !heading.isBlank()) printSubHeader(heading);
        System.out.println();
        if (events.isEmpty()) { printWarning("No events found."); return; }

        System.out.printf(BOLD
                + "  %-6s  %-32s  %-12s  %-7s  %-25s  %-8s  %-12s  %-9s%n" + RESET,
                "ID", "Event Name", "Date", "Time", "Location",
                "Max", "Registered", "Waitlist");
        printLine();

        for (Event e : events) {
            String sc = statusColour(e.getStatus());
            System.out.printf(
                    "  %s%-6d%s  %-32s  %-12s  %-7s  %-25s  %-8d  %s%-12s%s  %s%-9d%s%n",
                    sc, e.getEventId(), RESET,
                    truncate(e.getEventName(), 32),
                    e.getFormattedDate(),
                    e.getFormattedTime(),
                    truncate(e.getLocation(), 25),
                    e.getMaxParticipants(),
                    GREEN, e.getRegisteredCount() + "/" + e.getMaxParticipants(), RESET,
                    e.getWaitlistCount() > 0 ? YELLOW : "", e.getWaitlistCount(), RESET);
        }
        printLine();
        System.out.println("  " + GREEN + "■" + RESET + " Upcoming   "
                + CYAN   + "■" + RESET + " Ongoing   "
                + YELLOW + "■" + RESET + " Completed   "
                + RED    + "■" + RESET + " Cancelled");
        System.out.printf("  Total: %d event(s)%n", events.size());
    }

    /** Full event detail card — spec 2.4: full details including counts. */
    private void printEventDetail(Event e) {
        printLine();
        System.out.println(BOLD + CYAN + "  EVENT DETAILS" + RESET);
        printLine();
        System.out.printf("  %-22s %d%n",      "Event ID:",          e.getEventId());
        System.out.printf("  %-22s %s%n",      "Event Name:",        e.getEventName());
        System.out.printf("  %-22s %s%n",      "Date:",              e.getFormattedDate());
        System.out.printf("  %-22s %s%n",      "Time:",              e.getFormattedTime());
        System.out.printf("  %-22s %s%n",      "Location:",          e.getLocation());
        System.out.printf("  %-22s %s%s%s%n",  "Status:",
                statusColour(e.getStatus()), e.getStatus().getDisplayName(), RESET);
        System.out.printf("  %-22s %d%n",      "Max Participants:",  e.getMaxParticipants());
        System.out.printf("  %-22s %d / %d%n", "Registered:",
                e.getRegisteredCount(), e.getMaxParticipants());
        System.out.printf("  %-22s %d%n",      "Available Slots:",   e.getAvailableSlots());
        System.out.printf("  %-22s %d%n",      "Waitlist Count:",    e.getWaitlistCount());
        System.out.printf("  %-22s %s%n",      "Created By:",        e.getCreatedByStaff());
        printLine();
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // VALIDATED INPUT HELPERS  (spec 2.4 — validation & error handling)
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Read and validate an Event ID.
     * Loops with error feedback until a valid positive integer is entered.
     */
    private int readValidatedEventId() throws ValidationException {
        while (true) {
            String raw = readLine("Enter Event ID");
            try {
                return Event.Validator.validateEventId(raw);
            } catch (ValidationException e) {
                printError(e.getMessage());
                // Re-throw after 3 failures? For now loop — keeps UX simple
            }
        }
    }

    /**
     * Read and validate a non-blank text field.
     * Loops until a non-empty value is entered.
     */
    private String readValidatedNonBlank(String label) throws ValidationException {
        while (true) {
            String val = readLine(label);
            try {
                return Event.Validator.validateNonBlank(val, label);
            } catch (ValidationException e) {
                printError(e.getMessage());
            }
        }
    }

    /**
     * Read and validate a date in dd/MM/yyyy format.
     * Loops until a valid date is entered.
     */
    private LocalDate readValidatedDate(String label) {
        while (true) {
            String raw = readLine(label);
            try {
                return Event.Validator.validateDate(raw);
            } catch (ValidationException e) {
                printError(e.getMessage());
            }
        }
    }

    /**
     * Read and validate a time in HH:mm format.
     * Loops until a valid time is entered.
     */
    private LocalTime readValidatedTime(String label) {
        while (true) {
            String raw = readLine(label);
            try {
                return Event.Validator.validateTime(raw);
            } catch (ValidationException e) {
                printError(e.getMessage());
            }
        }
    }

    /**
     * Read and validate a positive integer.
     * Loops until a valid positive number is entered.
     */
    private int readValidatedPositiveInt(String label) {
        while (true) {
            String raw = readLine(label);
            try {
                return Event.Validator.validateMaxParticipants(raw);
            } catch (ValidationException e) {
                printError(e.getMessage());
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // PRINT / READ UTILITIES
    // ═════════════════════════════════════════════════════════════════════════════

    private void printBanner() {
        System.out.println(BOLD + BLUE +
                "╔══════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + BLUE + "║" + RESET + BOLD +
                "       RICHFIELD CAMPUS EVENT MANAGEMENT SYSTEM          " + BLUE + "║" + RESET);
        System.out.println(BOLD + BLUE + "║" + RESET + BOLD +
                "                    Version 1.0.0                        " + BLUE + "║" + RESET);
        System.out.println(BOLD + BLUE +
                "╚══════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    private void printHeader(String title) {
        System.out.println();
        System.out.println(BOLD + BLUE +
                "╔══════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(BOLD + BLUE + "║  " + RESET + BOLD
                + padRight(title, 55) + BLUE + "║" + RESET);
        System.out.println(BOLD + BLUE +
                "╚══════════════════════════════════════════════════════════╝" + RESET);
    }

    private void printSubHeader(String title) {
        if (title == null || title.isBlank()) return;
        System.out.println();
        System.out.println(BOLD + PURPLE + "  ┌─ " + title + " "
                + "─".repeat(Math.max(0, 54 - title.length())) + RESET);
    }

    private void printLine()              { System.out.println("  " + "─".repeat(76)); }
    private void printSuccess(String m)   { System.out.println("\n  " + GREEN  + "✔  " + m + RESET); }
    private void printError(String m)     { System.out.println("\n  " + RED    + "✖  " + m + RESET); }
    private void printWarning(String m)   { System.out.println("\n  " + YELLOW + "⚠  " + m + RESET); }

    private void pause() {
        System.out.print("\n  Press ENTER to continue...");
        scanner.nextLine();
    }

    private String readLine(String label) {
        System.out.print("  " + CYAN + label + ": " + RESET);
        return scanner.nextLine().trim();
    }

    private int readInt(String label) {
        System.out.print("  " + CYAN + label + ": " + RESET);
        try { return Integer.parseInt(scanner.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    private String statusColour(EventStatus s) {
        return switch (s) {
            case UPCOMING  -> GREEN;
            case ONGOING   -> CYAN;
            case COMPLETED -> YELLOW;
            case CANCELLED -> RED;
        };
    }

    private static String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max - 3) + "...";
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}

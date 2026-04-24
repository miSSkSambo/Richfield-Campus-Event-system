package src.services;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import src.models.*;

/**
 * Handles file-based persistence for the Campus Event Management System.
 *
 * Spec section 2.4:
 *   ✔ Save events, registrations, and waitlists to files
 *   ✔ Load all saved data automatically when the program starts
 *
 * File format (pipe-delimited plain text):
 *   events.txt       — one event per line
 *   registrations.txt — eventId|userId pairs (confirmed)
 *   waitlist.txt      — eventId|userId pairs (waitlisted, in order)
 *
 * All file I/O is wrapped in proper exception handling.
 */
public class PersistenceService {

    private static final String DATA_DIR         = "data";
    private static final String EVENTS_FILE      = DATA_DIR + "/events.txt";
    private static final String REGISTRATIONS_FILE = DATA_DIR + "/registrations.txt";
    private static final String WAITLIST_FILE    = DATA_DIR + "/waitlist.txt";

    private static final String DELIMITER = "|";
    private static final String SAFE_DELIM = "\\|";   // regex-safe split

    // ─── Ensure data directory exists ────────────────────────────────────────────

    public PersistenceService() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            System.err.println("[Persistence] Could not create data directory: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // SAVE
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Persist all events (with their registrations and waitlists) to disk.
     * Called after every mutating operation so data is never lost.
     *
     * @param events the current live list of all events
     */
    public void saveAll(List<Event> events) {
        saveEvents(events);
        saveRegistrations(events);
        saveWaitlists(events);
    }

    private void saveEvents(List<Event> events) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(EVENTS_FILE))) {
            // Header comment so the file is human-readable
            writer.write("# CEMS Events File — do not edit manually");
            writer.newLine();
            writer.write("# Format: id|name|date(dd/MM/yyyy)|time(HH:mm)|location|maxParticipants|status|createdBy");
            writer.newLine();

            for (Event e : events) {
                String line = String.join(DELIMITER,
                        String.valueOf(e.getEventId()),
                        escapePipe(e.getEventName()),
                        e.getFormattedDate(),
                        e.getFormattedTime(),
                        escapePipe(e.getLocation()),
                        String.valueOf(e.getMaxParticipants()),
                        e.getStatus().name(),
                        escapePipe(e.getCreatedByStaff()));
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException ex) {
            System.err.println("[Persistence] Failed to save events: " + ex.getMessage());
        }
    }

    private void saveRegistrations(List<Event> events) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REGISTRATIONS_FILE))) {
            writer.write("# CEMS Registrations — eventId|userId");
            writer.newLine();
            for (Event e : events) {
                for (String userId : e.getRegisteredParticipants()) {
                    writer.write(e.getEventId() + DELIMITER + userId);
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            System.err.println("[Persistence] Failed to save registrations: " + ex.getMessage());
        }
    }

    private void saveWaitlists(List<Event> events) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WAITLIST_FILE))) {
            writer.write("# CEMS Waitlists — eventId|userId (order preserved)");
            writer.newLine();
            for (Event e : events) {
                for (String userId : e.getWaitlistParticipants()) {
                    writer.write(e.getEventId() + DELIMITER + userId);
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            System.err.println("[Persistence] Failed to save waitlists: " + ex.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // LOAD
    // ═════════════════════════════════════════════════════════════════════════════

    /**
     * Load all saved events from disk.
     * Called automatically when the program starts.
     *
     * @return ordered map of eventId → Event, or empty map if files don't exist yet
     */
    @SuppressWarnings("UseSpecificCatch")
    public Map<Integer, Event> loadEvents() {
        Map<Integer, Event> events = new LinkedHashMap<>();
        File file = new File(EVENTS_FILE);
        if (!file.exists()) return events;   // first run — no data yet

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.startsWith("#") || line.isBlank()) continue;

                String[] parts = line.split(SAFE_DELIM, -1);
                if (parts.length < 8) {
                    System.err.printf("[Persistence] events.txt line %d: expected 8 fields, got %d — skipped%n",
                            lineNum, parts.length);
                    continue;
                }

                try {
                    int       id      = Integer.parseInt(parts[0].trim());
                    String    name    = unescapePipe(parts[1].trim());
                    LocalDate date    = LocalDate.parse(parts[2].trim(), Event.DATE_FORMAT);
                    LocalTime time    = LocalTime.parse(parts[3].trim(), Event.TIME_FORMAT);
                    String    loc     = unescapePipe(parts[4].trim());
                    int       maxP    = Integer.parseInt(parts[5].trim());
                    EventStatus status = EventStatus.valueOf(parts[6].trim());
                    String    staff   = unescapePipe(parts[7].trim());

                    Event event = new Event(id, name, date, time, loc, maxP, staff);
                    event.setStatus(status);
                    events.put(id, event);

                } catch (Exception ex) {
                    System.err.printf("[Persistence] events.txt line %d: parse error (%s) — skipped%n",
                            lineNum, ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.err.println("[Persistence] Could not read events.txt: " + ex.getMessage());
        }

        return events;
    }

    /**
     * Restore registrations from disk into the already-loaded event map.
     * Must be called AFTER loadEvents().
     */
    public void loadRegistrations(Map<Integer, Event> events) {
        loadParticipantFile(REGISTRATIONS_FILE, events, false);
    }

    /**
     * Restore waitlists from disk into the already-loaded event map.
     * Must be called AFTER loadEvents().
     */
    public void loadWaitlists(Map<Integer, Event> events) {
        loadParticipantFile(WAITLIST_FILE, events, true);
    }

    private void loadParticipantFile(String path, Map<Integer, Event> events, boolean isWaitlist) {
        File file = new File(path);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.startsWith("#") || line.isBlank()) continue;

                String[] parts = line.split(SAFE_DELIM, 2);
                if (parts.length < 2) {
                    System.err.printf("[Persistence] %s line %d: malformed — skipped%n", path, lineNum);
                    continue;
                }

                try {
                    int    eventId = Integer.parseInt(parts[0].trim());
                    String userId  = parts[1].trim();
                    Event  event   = events.get(eventId);

                    if (event == null) {
                        System.err.printf("[Persistence] %s line %d: event %d not found — skipped%n",
                                path, lineNum, eventId);
                        continue;
                    }

                    // Directly insert into the appropriate list (bypass registerUser logic
                    // to avoid status checks on already-saved data)
                    if (isWaitlist) {
                        event.loadWaitlistEntry(userId);
                    } else {
                        event.loadRegisteredEntry(userId);
                    }

                } catch (NumberFormatException ex) {
                    System.err.printf("[Persistence] %s line %d: invalid event ID — skipped%n",
                            path, lineNum);
                }
            }
        } catch (IOException ex) {
            System.err.println("[Persistence] Could not read " + path + ": " + ex.getMessage());
        }
    }

    // ─── High watermark — so the ID counter picks up where it left off ───────────

    /**
     * Returns the highest event ID found in saved data.
     * EventService uses this so new events don't reuse old IDs.
     */
    public int getMaxSavedEventId(Map<Integer, Event> events) {
        return events.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    // ─── Pipe escaping helpers (prevent delimiter collisions in names/locations) ──

    private String escapePipe(String s) {
        return s == null ? "" : s.replace("|", "{{PIPE}}");
    }

    private String unescapePipe(String s) {
        return s == null ? "" : s.replace("{{PIPE}}", "|");
    }

    // ─── Utility: check if save files exist ───────────────────────────────────────
    public boolean hasSavedData() {
        return new File(EVENTS_FILE).exists();
    }
}

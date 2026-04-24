# Richfield Campus Event Management System (CEMS)
### PRO731 Java Programming Assignment — Version 1.0.0

<img width="1577" height="933" alt="Screenshot 2026-04-24 130922" src="https://github.com/user-attachments/assets/f045965c-7bfb-45f0-a101-cceda05df0b5" />


---

## Table of Contents
1. [Project Overview](#1-project-overview)
2. [Project Structure](#2-project-structure)
3. [How to Compile and Run](#3-how-to-compile-and-run)
4. [How to Use the System](#4-how-to-use-the-system)
5. [Features](#5-features)
6. [OOP Design & Technical Requirements](#6-oop-design--technical-requirements)
7. [Data Persistence](#7-data-persistence)
8. [Exception Handling & Validation](#8-exception-handling--validation)
9. [Sample Data](#9-sample-data)

---

## 1. Project Overview

The **Richfield Campus Event Management System (CEMS)** is a Java console-based application that allows students and staff to create, manage, view, search, and register for campus events.

The system simulates real-world behaviour including:
- Role-based access control (Staff vs Student)
- Event capacity management with automatic waitlisting
- Automated notifications on registration and event changes
- Full data persistence across sessions using file I/O

---

## 2. Project Structure

```
JAVA Assignment/
│
├── src/
│   ├── Main.java                            ← Application entry point
│   │
│   ├── models/                              ← Data model classes
│   │   ├── User.java                        ← Abstract base class (inheritance)
│   │   ├── StaffUser.java                   ← Staff subclass (polymorphism)
│   │   ├── StudentUser.java                 ← Student subclass (polymorphism)
│   │   ├── Event.java                       ← Event entity + inner Validator class
│   │   ├── Role.java                        ← Enum: STAFF, STUDENT
│   │   ├── EventStatus.java                 ← Enum: UPCOMING, ONGOING, COMPLETED, CANCELLED
│   │   ├── EventCategory.java               ← Enum: ACADEMIC, SPORTS, CULTURAL, etc.
│   │   ├── Notification.java                ← Notification message model
│   │   ├── ValidationException.java         ← Custom exception: invalid input format
│   │   ├── InvalidEventException.java       ← Custom exception: business rule violated
│   │   ├── EventNotFoundException.java      ← Custom exception: event ID not found
│   │   └── DuplicateRegistrationException.java ← Custom exception: already registered
│   │
│   ├── services/                            ← Business logic layer
│   │   ├── EventService.java                ← Core event CRUD, search, registration
│   │   ├── UserService.java                 ← Session user factory
│   │   ├── NotificationService.java         ← Automated notification delivery
│   │   └── PersistenceService.java          ← File save/load (I/O)
│   │
│   └── ui/
│       └── ConsoleUI.java                   ← Console menu interface
│
├── out/                                     ← Compiled .class files (auto-generated)
│
├── data/                                    ← Persistence files (auto-generated)
│   ├── events.txt                           ← Saved events
│   ├── registrations.txt                    ← Saved confirmed registrations
│   └── waitlist.txt                         ← Saved waitlist entries
│
└── README.md                                ← This file
```

---

## 3. How to Compile and Run

### Requirements
- Java Development Kit (JDK) **17 or later**
- A terminal or command prompt opened in the `JAVA Assignment\` root folder

### Compile (PowerShell or Command Prompt)

```powershell
javac -d out src\models\*.java src\services\*.java src\ui\*.java src\Main.java
```

### Run

```powershell
java -cp out src.Main
```

### Run in VS Code
1. Open the `JAVA Assignment` folder in VS Code
2. Install the **Extension Pack for Java**
3. Open `src/Main.java`
4. Click the **▷ Run** button above the `main` method
5. Interact with the program in the terminal panel at the bottom

> **Note:** The `data/` and `out/` folders are created automatically on first run.

---

## 4. How to Use the System

### Step 1 — Select Your Role
When the program starts you will be prompted to choose:
```
  Select your role to continue:
  1. Staff
  2. Student
```
Enter `1` for Staff or `2` for Student, then enter your name.

---

### Staff Menu

| Option | Action |
|--------|--------|
| 1 | View all events (with sort) |
| 2 | Search events (by name or date) |
| 3 | Create a new event |
| 4 | Update event details |
| 5 | Cancel an event |
| 6 | View participants & waitlist |
| 0 | Exit |

#### Creating an Event
You will be prompted for:
- **Event Name** — any non-empty text
- **Event Date** — format `dd/MM/yyyy` e.g. `25/12/2025`
- **Event Time** — format `HH:mm` e.g. `14:30`
- **Location** — any non-empty text
- **Maximum Participants** — a positive whole number

The system assigns a unique integer Event ID automatically.

#### Sorting Events
Before every event list is displayed, you can choose to sort by:
- `1` — Event Name (A → Z)
- `2` — Event Date (earliest first)
- `3` — No sort (insertion order)

#### Cancelling an Event
Type `CONFIRM` when prompted to proceed. All registered and waitlisted users are notified automatically.

---

### Student Menu

| Option | Action |
|--------|--------|
| 1 | View all upcoming events |
| 2 | Search events (by name or date) |
| 3 | Register for an event |
| 4 | Cancel my registration |
| 5 | View my registration status |
| 6 | View my notifications |
| 0 | Exit |

#### Registration Status
Each event you are linked to shows one of two statuses:
- ✔ **Registered** — you have a confirmed spot
- ⏳ **Waitlisted #N** — you are position N in the waitlist queue

#### Automatic Waitlist Promotion
If a confirmed participant cancels, the first person on the waitlist is automatically moved to confirmed and notified.

---

### Searching for Events
Both Staff and Students can search:

| Search Type | How it works |
|-------------|-------------|
| By Event Name | Partial or full match — `"sci"` finds `"AI & Machine Learning Bootcamp"` |
| By Event Date | Exact date match — enter `dd/MM/yyyy` |

Full event details (including registration count and waitlist count) are displayed for every result.

---

## 5. Features

### Role-Based Access Control
- **Staff** can create, update, cancel events and view all participants
- **Students** can only view, search, register, cancel their own registration, and check status
- Students have zero access to event management options — those menu items do not appear

### Event Lifecycle
Events automatically progress through states based on the current date:

| Status | Meaning |
|--------|---------|
| `UPCOMING` | Event has not started yet |
| `ONGOING` | Event is happening today |
| `COMPLETED` | Event date has passed |
| `CANCELLED` | Manually cancelled by staff |

### Capacity Management
- Events have a configurable **Maximum Participants** limit
- When an event is full, new registrations are automatically placed on the **Waitlist**
- The waitlist is a FIFO (First-In, First-Out) queue — earliest registrant gets promoted first

### Notifications
Automatic notifications are sent when:
- A student registers for an event (confirmed or waitlisted)
- A student is promoted from the waitlist to confirmed
- A student cancels their registration
- An event is updated (notifies all registered participants)
- An event is cancelled (notifies all registered and waitlisted users)

---

## 6. OOP Design & Technical Requirements

### Inheritance & Polymorphism

```
User  (abstract base class)
├── StaffUser   — canManageEvents() returns true
└── StudentUser — canManageEvents() returns false
```

- The `User` class defines three **abstract methods**: `getMenuTitle()`, `canManageEvents()`, `getPermissionSummary()`
- `StaffUser` and `StudentUser` **override** these methods with role-specific behaviour
- The UI uses the abstract `User` reference — the correct menu is shown based on the **runtime type** of the object (polymorphism)
- `UserService.createSessionUser()` acts as a **factory method**, returning the correct subclass

### Data Structures

| Structure | Used For |
|-----------|---------|
| `LinkedHashMap<Integer, Event>` | Main event store — O(1) lookup by ID, preserves insertion order |
| `ArrayList<String>` | Registered participant list inside each Event |
| `LinkedList<String>` (as Queue) | Waitlist inside each Event — FIFO promotion |
| `ArrayList<Notification>` | Notification inbox per user |

### Custom Exception Hierarchy

| Exception | When Thrown |
|-----------|-------------|
| `ValidationException` | Blank field, wrong date/time format, non-numeric ID |
| `InvalidEventException` | Past date, already cancelled, capacity ≤ 0 |
| `EventNotFoundException` | Requested Event ID does not exist |
| `DuplicateRegistrationException` | Student already registered or waitlisted |

### Class Responsibilities (Separation of Concerns)

| Layer | Classes | Responsibility |
|-------|---------|---------------|
| Model | `Event`, `User`, `StaffUser`, `StudentUser`, `Notification` | Data and business state |
| Service | `EventService`, `UserService`, `NotificationService`, `PersistenceService` | Business logic and I/O |
| UI | `ConsoleUI` | User interaction only — no business logic |

---

## 7. Data Persistence

All data is saved to plain text files in the `data/` folder automatically after every change.

### File Format

**`data/events.txt`**
```
# Format: id|name|date(dd/MM/yyyy)|time(HH:mm)|location|maxParticipants|status|createdBy
1|AI & Machine Learning Bootcamp|27/04/2026|10:00|Computer Lab B|60|UPCOMING|System
2|Entrepreneurship Pitch Night|29/04/2026|17:00|Innovation Hub|80|UPCOMING|System
```

**`data/registrations.txt`**
```
# Format: eventId|userId
1|U1
3|U2
```

**`data/waitlist.txt`**
```
# Format: eventId|userId (order preserved — first line = first in queue)
3|U3
```

### Load Behaviour
- On **first run**: sample events are seeded automatically
- On **subsequent runs**: all saved events, registrations, and waitlists are loaded from the files
- The Event ID counter resumes from the highest saved ID — no IDs are ever reused

---

## 8. Exception Handling & Validation

All user input is validated before any operation is performed. The system **never crashes** on bad input — errors are displayed clearly and the user is re-prompted.

### Validated Fields

| Field | Rules |
|-------|-------|
| Event ID | Must be a positive whole number — letters cause a clear error message |
| Event Name | Cannot be blank, maximum 100 characters |
| Event Date | Must be in `dd/MM/yyyy` format and not in the past |
| Event Time | Must be in `HH:mm` format (24-hour) |
| Location | Cannot be blank |
| Max Participants | Must be a positive whole number greater than zero |

### Error Examples

```
✖  "abc" is not a valid number. Event ID must be a whole number.
✖  Event date 01/01/2020 is in the past. Please choose a future date.
✖  "25-12-2025" is not a valid date. Use dd/MM/yyyy (e.g. 25/12/2025).
✖  You are already registered for " AI & Machine Learning Bootcamp".
✖  No event found with ID: 99
```

---

## 9. Sample Data

Five events are loaded automatically on first run:


| ID | Event Name | Date | Time | Location | Capacity |
|----|-----------|------|------|----------|---------|
| 1 | AI & Machine Learning Bootcamp | 27/04/2026 | 10:00 | Computer Lab B | 60 |
| 2 | Entrepreneurship Pitch Night | 29/04/2026 | 17:00 | Innovation Hub | 80 |
| 3 | Cybersecurity Awareness Day | 02/05/2026 | 09:00 | Lecture Hall C | 120 |
| 4 | Mobile App Development Workshop | 04/05/2026 | 13:00 | IT Building Room 2 | 40 |
| 5 | Cultural Food Festival | 07/05/2026 | 12:00 | Campus Courtyard | 200 |

> Dates are relative to the day the program is first run.

---

*Richfield Campus Event Management System — PRO731 Java Programming Assignment*

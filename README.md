# Smart College Event & Registration Management System

A Java 17 + SQLite + JDBC console application demonstrating the design patterns from Design Patterns Lab Practical 1-4.

## Design Patterns

| Pattern | Project Usage |
|---|---|
| Singleton | One central `EventManagementSystem` |
| Chain of Responsibility | Coordinator → Faculty Coordinator → HOD approval |
| Factory Method | Create Technical/Cultural/Sports/Workshop events |
| Proxy | Student authentication before system access |
| Abstract Factory | Create category-specific event families |
| Bridge | Email/SMS/Mobile App notifications |
| Observer | Student, Faculty and Event Coordinator notifications |
| JDBC + SQLite | Persistent CRUD operations |

## Features

- Student registration and login
- View events
- Create events using Factory Method
- Event-category families using Abstract Factory
- Event registration
- Approval workflow using Chain of Responsibility
- Proxy authentication
- Email/SMS/Mobile App notification simulation
- Observer-based status notifications
- SQLite database with JDBC
- CRUD for students, events and registrations
- Seed data for quick demonstration
- JUnit tests

## Requirements

- JDK 17+
- Maven 3.9+

## Run

```bash
mvn clean test
mvn package
java -jar target/smart-college-event-management-1.0.0.jar
```

Or:

```bash
mvn exec:java -Dexec.mainClass="com.college.event.Main"
```

The SQLite database `college_events.db` is created automatically in the project root and is ignored by Git.

## Demo Accounts

- Student: `student1` / `1234`
- Student: `student2` / `1234`
- Coordinator: `coordinator` / `admin`
- Faculty: `faculty` / `admin`
- HOD: `hod` / `admin`

## Suggested Demo

1. Login as `student1`.
2. View available events.
3. Register for an event.
4. The system runs the approval chain.
5. Select Email, SMS or Mobile App notification.
6. View your registrations.
7. Login as coordinator and create a new event.
8. View the database records.

## Project Structure

```text
src/
 ├── main/java/com/college/event/
 │   ├── Main.java
 │   ├── model/
 │   ├── database/
 │   └── patterns/
 │       ├── factory/
 │       ├── abstractfactory/
 │       ├── proxy/
 │       ├── singleton/
 │       ├── chain/
 │       ├── bridge/
 │       └── observer/
 └── test/java/com/college/event/
```

## Database

Tables are created automatically:

- `students`
- `events`
- `registrations`
- `notifications`

The application seeds sample accounts and events only when the database is empty.

## Academic Mapping

### Practical 1
Singleton + Chain of Responsibility:
- `EventManagementSystem`
- `ApprovalHandler`, `EventCoordinator`, `FacultyCoordinator`, `HOD`

### Practical 2
Factory Method + Proxy:
- `EventFactory`, concrete event creators
- `EventSystemProxy`

### Practical 3
Abstract Factory + Bridge:
- `EventFamilyFactory`
- `TechnicalEventFactory`, `CulturalEventFactory`, `SportsEventFactory`
- `NotificationChannel`

### Practical 4
Observer + JDBC:
- `EventSubject`, `StudentObserver`, `FacultyObserver`, `CoordinatorObserver`
- `DatabaseManager`


## Refined User Flow

The application now has a proper student onboarding flow:

1. **Student Registration** – create a new account with name, username, password, email and department.
2. **Student Login** – authenticate using the newly created account.
3. **Browse Events** – view event details and remaining seats.
4. **Register for Event** – select an event by its displayed number; no database IDs are required from the student.
5. **My Registrations** – view registration status.
6. **Cancel Registration** – cancel only the logged-in student's own registration.
7. **Coordinator Panel** – protected by coordinator code `admin123` for event creation and system demonstrations.

### Fresh Database Note

If you previously ran an older version, `college_events.db` may already contain the old seeded students/events. Delete `college_events.db` once before the first run of this refined version if you want a completely fresh database.

### Example Student Registration

From the main menu choose:

`1. Student Registration`

Then enter:

```text
Full name: Rahul Sharma
Choose username: rahul01
Create password: 1234
Confirm password: 1234
Email: rahul01@college.edu
Department: CSE
```

The account is stored in SQLite and can immediately be used through **Student Login**.

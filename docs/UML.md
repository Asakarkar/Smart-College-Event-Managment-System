# UML Class Diagram (Conceptual)

```text
                         Event
                           ^
                           |
                  +--------+--------+
                  |                 |
             BaseEvent        Event Factory
                                    |
                 +------------------+----------------+
                 |        |          |               |
             Technical  Cultural    Sports        Workshop

Student --> EventSystemProxy --> RealEventSystem
                                      |
                                      v
                             EventManagementSystem
                                  <<Singleton>>

ApprovalHandler
      |
      +--> EventCoordinator --> FacultyCoordinator --> HOD

EventFamilyFactory
      |
      +--> TechnicalEventFactory
      +--> CulturalEventFactory
      +--> SportsEventFactory

EventNotifier --> NotificationChannel
                       |
             +---------+---------+
             |         |         |
           Email      SMS     MobileApp

EventSubject
      |
      +--> EventCoordinatorNotifier
                  |
          +-------+-------+ 
          |       |       |
       Student Faculty Coordinator
        Observer Observer Observer

DatabaseManager --> SQLite/JDBC
```

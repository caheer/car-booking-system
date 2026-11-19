# Car Booking System — Design Trade-offs and Known Limitations

## Design Trade-offs

- **Candidate selection without automatic retry.** The application first identifies an available car, then acquires a pessimistic lock and rechecks availability within the transaction. If another transaction has booked that car in the meantime, the request is rejected rather than searching for another car. This keeps the booking flow straightforward, at the cost of occasionally rejecting a request when another suitable car may still be available.

- **Database-backed implementation.** Spring Data JPA and a relational database introduce a database dependency compared with an in-memory approach, but provide persistence and transactional row locking without requiring custom synchronization infrastructure.

- **Database-side availability selection.** The main availability check is performed in SQL, avoiding the need to retrieve and process potentially large numbers of cars and bookings in Java. The trade-off is that changes to the selection algorithm may require modifying a more complex query.

- **Native SQL for candidate selection.** The availability query uses native SQL, including `LIMIT 1`, which is not supported directly by JPQL. This reduces portability across database dialects. A JPQL query with Spring Data pagination is an alternative, but was not considered beneficial for this exercise.

- **Immediate vehicle assignment.** A successful reservation is assigned to a specific car rather than reserving a category and assigning a vehicle later. Cars of the same type are treated as interchangeable; individual vehicle attributes and selection preferences are outside the current model.

- **Focused data model.** The model includes only the information needed for the requested booking workflow. Customer details and booking creation timestamps could be added without fundamentally changing the existing car-to-booking relationship.

- **Time representation.** Booking times use `LocalDateTime` and assume a common business time zone. Supporting multiple regions would require an explicit time-zone policy.

- **Calculating rental period.** Each 24h 'day' of car begins at the exact precise moment in time specified in the requested booking date input, and ends 24h later. The assignment does not specify rental-day calculation rules, although in real life scenarios such specifics would usually be clarified with the client before implementation.

## Known Limitations

- **All booking writers must follow the locking protocol.** Prevention of overlapping reservations depends on acquiring the relevant car lock and rechecking availability before inserting a booking. Direct database inserts, bulk operations or external writers that bypass this protocol are not independently prevented from creating overlaps by a database constraint.

- **Database concurrency semantics matter.** The implementation relies on the database's pessimistic locking and transaction isolation behavior. These assumptions should be verified for the chosen database and isolation level, particularly if the database configuration changes.

- **Concurrent database transactions have not been tested directly.** Unit tests cover service decisions and failure paths, while repository tests verify interval overlaps and boundary conditions. Concurrent booking behavior has not been explicitly verified using simultaneous database transactions.

- **Controller tests are not included.** REST endpoint behavior has not been verified through dedicated MockMvc tests. Input validation is implemented using Jakarta Bean Validation annotations, but has not been tested separately.
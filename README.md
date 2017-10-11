# Introduction

This is a sample Akka/Java application that has a Customer service that allows you, via REST, to:

1. Add a customer
2. Get a customer
3. List customers (via read-side view)
4. Disable a customer (acting as a soft delete)

This implementation uses Cassandra for both the write-side and read-side.

# Example curl commands

1. curl -H "Content-Type: application/json" -X POST -d '{"name": "Eric Murphy", "city": "San Francisco", "state": "CA", "zipCode": "94105"}' http://localhost:8080/customer
2. curl http://localhost:8080/customer/51c25a39-39b8-4937-b56b-5cca7f79acc1
3. curl http://localhost:8080/customer
4. curl -X POST http://localhost:8080/customer/disable/51c25a39-39b8-4937-b56b-5cca7f79acc1
5. curl http://localhost:8080/customer/51c25a39-39b8-4937-b56b-5cca7f79acc1 (run again to check disabled)

# Caveats (may be revisited)

1. Could not get Akka Persistence Query readJournal.eventsByTag working which would improve the read-side implementation with an offset
2. Using raw underlying Cassandra Session for read-side since I had trouble making a new Akka Persistence CassandraSession. Should not reuse the same one from the write-side
3. Unsure if interaction between Akka HTTP and Akka Persistence actors is optimal. For example, should I destroy the actor after the request is completed?
4. getCustomer should probably use the read-side rather than read from the Akka Persistence. However that is what I did with the Lagom implementation with the PersistentEntity




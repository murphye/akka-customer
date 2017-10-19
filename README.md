# Introduction

This is a sample Akka/Java application that has a Customer service that allows you, via REST, to:

1. Add a customer
2. Get a customer
3. List customers (via read-side view)
4. Disable a customer (acting as a soft delete)

This implementation uses Cassandra for both the write-side and read-side.

# Useful SBT Commands

* `~reStart`, `reStop` [Triggered Restarts via SBT Revolver](https://github.com/spray/sbt-revolver) (Restart app on source changes)
* `docker:publishLocal` [JAR and Docker images via SBT Native Packager](https://github.com/sbt/sbt-native-packager)

# Example curl commands

1. curl -H "Content-Type: application/json" -X POST -d '{"name": "Eric Murphy", "city": "San Francisco", "state": "CA", "zipCode": "94105"}' http://localhost:8080/customer
2. curl http://localhost:8080/customer/51c25a39-39b8-4937-b56b-5cca7f79acc1
3. curl http://localhost:8080/customer
4. curl -X PUT http://localhost:8080/customer/disable/51c25a39-39b8-4937-b56b-5cca7f79acc1
5. curl http://localhost:8080/customer/51c25a39-39b8-4937-b56b-5cca7f79acc1 (run again to check disabled)

# TODOs

* Akka Cluster sharding for Persistent Actors
* Improve code retrieving underlying Cassandra Session for read-side 
* Switch from default Java serializer to ProtoBuf or something better



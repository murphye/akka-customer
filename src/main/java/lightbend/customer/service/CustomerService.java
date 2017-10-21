package lightbend.customer.service;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.ExceptionHandler;
import akka.http.javadsl.server.Route;
import akka.util.Timeout;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableList;
import lightbend.customer.api.Customer;
import lightbend.customer.persistence.CustomerCommand;
import lightbend.customer.persistence.CustomerEventProcessor;
import lightbend.customer.persistence.CustomerPersistentActor;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.server.Directives.*;
import static akka.pattern.PatternsCS.ask;

public class CustomerService {

    private final ActorSystem actorSystem;

    private final CustomerEventProcessor customerEventProcessor;

    private final ActorRef customerPersistentActor;

    private Session cassandraSession;

    private final Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(100, TimeUnit.MILLISECONDS));

    public CustomerService(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
        
        ClusterShardingSettings clusterShardingSettings = ClusterShardingSettings.create(actorSystem);

        this.customerPersistentActor = ClusterSharding.get(this.actorSystem).start("CustomerPersistentActor",
                Props.create(CustomerPersistentActor.class), clusterShardingSettings, CustomerPersistentActor.shardExtractor());

        this.customerEventProcessor = new CustomerEventProcessor(actorSystem);

        // Use the same read-side Cassandra session as the event processor which connects to same cluster/keyspace
        this.customerEventProcessor.getCassandraSession().thenAccept(s ->
                this.cassandraSession = s
        );
    }

    public Route addCustomer(Customer customer) {
        CustomerCommand.AddCustomer addCustomer = new CustomerCommand.AddCustomer(customer);
        customerPersistentActor.tell(addCustomer, ActorRef.noSender());

        return complete(StatusCodes.ACCEPTED, "Customer added");
    }

    public Route getCustomer(String customerId) {
        CustomerCommand.GetCustomer getCustomer = new CustomerCommand.GetCustomer(customerId);
        CompletionStage<Customer> addCustomerResult = ask(customerPersistentActor, getCustomer, timeout).thenApply((Customer.class::cast));

        final ExceptionHandler exceptionHandler = ExceptionHandler.newBuilder()
                .match(CompletionException.class, x -> {
                    x.printStackTrace();
                        return complete(StatusCodes.NOT_FOUND, "Customer is disabled");
                })
                .build();

        return handleExceptions(exceptionHandler, () ->
                completeOKWithFuture(addCustomerResult, Jackson.<Customer>marshaller()));
    }

    public Route disableCustomer(String customerId) {
        CustomerCommand.DisableCustomer disableCustomer = new CustomerCommand.DisableCustomer(customerId);
        customerPersistentActor.tell(disableCustomer, ActorRef.noSender());

        return complete(StatusCodes.ACCEPTED, "Customer disabled");
    }

    public Route getCustomers() {
        // Creating a new CassandraSession doesn't seem to be supported with Akka Java DSL, so for now,
        // reverting to using underlying Session object to synchronously retrieve the results

        ResultSet resultSet = this.cassandraSession.execute("SELECT id, name, city, state, zipcode FROM customers.customer");
        ImmutableList.Builder<Customer> builder = ImmutableList.builder();

        while (resultSet.iterator().hasNext()) {
            Row row = resultSet.iterator().next();
            builder.add(
                Customer.builder().id(row.getString("id"))
                        .name(row.getString("name"))
                        .city(row.getString("city"))
                        .state(row.getString("state"))
                        .zipCode(row.getString("zipcode"))
                        .build()
            );
        }

        return completeOK(builder.build(), Jackson.<ImmutableList<Customer>>marshaller());
    }
}

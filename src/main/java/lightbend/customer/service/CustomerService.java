package lightbend.customer.service;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.dispatch.MessageDispatcher;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.server.Directives.*;
import static akka.pattern.PatternsCS.ask;

/**
 * Implement the CustomerService interface to add, disable, get, and return all customers.
 */
public class CustomerService {

    private final ActorSystem actorSystem;

    private final ActorRef customerPersistentActor;

    private Session readSideCassandraSession;

    /**
     * Constuctor which will setup cluster sharding and read-side event processor for this service.
     * @param actorSystem The actor system
     */
    public CustomerService(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;

        ClusterShardingSettings clusterShardingSettings = ClusterShardingSettings.create(actorSystem);

        this.customerPersistentActor = ClusterSharding.get(this.actorSystem).start("CustomerPersistentActor",
                Props.create(CustomerPersistentActor.class), clusterShardingSettings, CustomerPersistentActor.shardExtractor());

        CustomerEventProcessor  customerEventProcessor = new CustomerEventProcessor(actorSystem);

        // Get the read-side Cassandra session from the event processor
        customerEventProcessor.getCassandraSession().thenAccept(s ->
                this.readSideCassandraSession = s
        );
    }

    /**
     * Add a new customer as part of the HTTP Request body in JSON format, and not containing the UUID.
     * @return Confirmation the command has succeeded.
     */
    public Route addCustomer(Customer customer) {
        CustomerCommand.AddCustomer addCustomer = new CustomerCommand.AddCustomer(customer);
        customerPersistentActor.tell(addCustomer, ActorRef.noSender());

        return complete(StatusCodes.ACCEPTED, "Customer added");
    }

    /**
     * Return a customer from its known UUID directly from the entity.
     * @param customerId UUID of the customer.
     * @return The customer.
     */
    public Route getCustomer(String customerId) {
        CustomerCommand.GetCustomer getCustomer = new CustomerCommand.GetCustomer(customerId);
        Timeout timeout = Timeout.durationToTimeout(FiniteDuration.apply(100, TimeUnit.MILLISECONDS));
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

    /**
     * Disable a customer, which effectively does a soft delete, in that the customer record will no longer be visible.
     * @param customerId UUID of the customer.
     * @return Confirmation the command has succeeded.
     */
    public Route disableCustomer(String customerId) {
        CustomerCommand.DisableCustomer disableCustomer = new CustomerCommand.DisableCustomer(customerId);
        customerPersistentActor.tell(disableCustomer, ActorRef.noSender());

        return complete(StatusCodes.ACCEPTED, "Customer disabled");
    }

    /**
     * Return the customers from the read-side view.
     * @return A list of customers.
     */
    public Route getCustomers() {
        final MessageDispatcher dispatcher = actorSystem.dispatchers().lookup("http-blocking-dispatcher");

        // Execute blocking code inside of a CompletableFuture to isolate or "bulkhead" blocking operations
        return completeOKWithFuture(CompletableFuture.supplyAsync(() -> {

                    // WARNING: You should use executeAsync but using blocking call here as an example to show how to use dispatcher
                    ResultSet resultSet = this.readSideCassandraSession.execute("SELECT id, name, city, state, zipcode FROM customers.customer");
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

                    return builder.build();
                }, dispatcher // uses the "blocking dispatcher" that we configured, instead of the default dispatcher to isolate the blocking.
        ), Jackson.<ImmutableList<Customer>>marshaller());
    }
}

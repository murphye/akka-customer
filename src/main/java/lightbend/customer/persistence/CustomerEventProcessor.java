package lightbend.customer.persistence;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.persistence.cassandra.query.javadsl.CassandraReadJournal;
import akka.persistence.cassandra.session.javadsl.CassandraSession;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.PersistenceQuery;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;
import com.datastax.driver.core.*;
import lightbend.customer.api.Customer;

import java.util.concurrent.CompletionStage;

/**
 * Implement the CassandraReadSide processor to capture the CustomerEvents and either
 * add or remove customers from the read-side view.
 */
public class CustomerEventProcessor {

    private CassandraReadJournal readJournal;

    private Source<String, NotUsed> sourceIds;

    private Session cassandraSession;

    public CustomerEventProcessor(final ActorSystem actorSystem) {

        this.readJournal = PersistenceQuery.get(actorSystem).getReadJournalFor(CassandraReadJournal.class, CassandraReadJournal.Identifier());

        getCassandraSession().thenAccept(s -> {
            // Create a new Cassandra session to be used for the read-side
            Session cassandraSession = s.getCluster().newSession();
            this.cassandraSession = cassandraSession;

            // Check read-side state
            KeyspaceMetadata keyspaceMetadata = s.getCluster().getMetadata().getKeyspace("customers"); // TODO: Get keyspace from config
            if(keyspaceMetadata == null || keyspaceMetadata.getTable("customer") == null) {
                createKeyspace();
                createTable();
            }
            else {
                clearTable();
            }

            // Continuous stream of persistenceIds
            this.sourceIds = readJournal.persistenceIds();

            sourceIds.runForeach(persistenceId -> {
                // Continuous stream of events by persistenceId
                Source<EventEnvelope, NotUsed> sourceEvents = readJournal.eventsByPersistenceId(persistenceId, 0, Long.MAX_VALUE);

                sourceEvents.runForeach(eventEnvelope -> {
                    CustomerEvent event = (CustomerEvent) eventEnvelope.event();

                    if(event instanceof CustomerEvent.CustomerAdded) {
                        processCustomerAdded((CustomerEvent.CustomerAdded)event);
                    }
                    else if(event instanceof CustomerEvent.CustomerDisabled) {
                        processCustomerDisabled((CustomerEvent.CustomerDisabled)event);
                    }

                }, ActorMaterializer.create(actorSystem));

            }, ActorMaterializer.create(actorSystem));
        });
    }

    public CompletionStage<Session> getCassandraSession() {
        return this.readJournal.session().underlying();
    }

    private ResultSet createKeyspace() {
        return cassandraSession.execute("CREATE KEYSPACE IF NOT EXISTS customers " +
                "WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1}; "
        );
    }

    private ResultSet createTable() {
        return cassandraSession.execute("CREATE TABLE IF NOT EXISTS customers.customer ( " +
                "id TEXT, name TEXT, city TEXT, state TEXT, zipcode TEXT, PRIMARY KEY (id))"
        );
    }

    private ResultSet clearTable() {
        return cassandraSession.execute("TRUNCATE customers.customer");
    }

    private void processCustomerAdded(CustomerEvent.CustomerAdded customerAddedEvent) {
        Customer customer = customerAddedEvent.getCustomer();

        PreparedStatement ps = cassandraSession.prepare("INSERT INTO customers.customer (id, name, city, state, zipcode) VALUES (?, ?, ?, ?, ?)");
        BoundStatement bindWriteCustomer = ps.bind()
                .setString("id", customer.getId())
                .setString("name", customer.getName())
                .setString("city", customer.getCity())
                .setString("state", customer.getState())
                .setString("zipcode", customer.getZipCode());

        cassandraSession.execute(bindWriteCustomer);
    }

    private void processCustomerDisabled(CustomerEvent.CustomerDisabled customerDisabledEvent) {
        PreparedStatement ps = cassandraSession.prepare("DELETE FROM customers.customer WHERE id=?");

        BoundStatement bindDisableCustomer = ps.bind();
        bindDisableCustomer.setString("id", customerDisabledEvent.getCustomer().getId());

        cassandraSession.execute(bindDisableCustomer);
    }
}
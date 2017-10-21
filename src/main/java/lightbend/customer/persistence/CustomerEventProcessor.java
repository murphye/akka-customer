package lightbend.customer.persistence;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.persistence.cassandra.query.javadsl.CassandraReadJournal;
import akka.persistence.cassandra.session.javadsl.CassandraSession;
import akka.persistence.query.EventEnvelope;
import akka.persistence.query.Offset;
import akka.persistence.query.PersistenceQuery;
import akka.persistence.query.TimeBasedUUID;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;
import com.datastax.driver.core.*;
import com.datastax.driver.core.utils.UUIDs;
import lightbend.customer.api.Customer;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Implement the CassandraReadSide processor to capture the CustomerEvents and either
 * add or remove customers from the read-side view for this CQRS implementation. Store the offset in the read-side
 * so we only have to load the latest events if the application is restarted.
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
                createOffsetTable();
                createTable();
            }

            Offset offset = getOffset(CustomerEvent.TAG); // Retrieved the stored offset

            // Loop through events by tag from the offset.
            readJournal.eventsByTag(CustomerEvent.TAG, offset).runForeach(eventEnvelope -> {
                    CustomerEvent event = (CustomerEvent) eventEnvelope.event();

                    updateOffset(CustomerEvent.TAG, ((TimeBasedUUID)eventEnvelope.offset()).value());

                    if(event instanceof CustomerEvent.CustomerAdded) {
                        processCustomerAdded((CustomerEvent.CustomerAdded)event);
                    }
                    else if(event instanceof CustomerEvent.CustomerDisabled) {
                        processCustomerDisabled((CustomerEvent.CustomerDisabled)event);
                    }

                }, ActorMaterializer.create(actorSystem));
        });
    }

    public CompletionStage<Session> getCassandraSession() {
        return this.readJournal.session().underlying();
    }

    private void createKeyspace() {
        cassandraSession.execute("CREATE KEYSPACE IF NOT EXISTS customers " +
                "WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1}; "
        );
    }

    private void createTable() {
        cassandraSession.execute("CREATE TABLE IF NOT EXISTS customers.customer ( " +
                "id TEXT, name TEXT, city TEXT, state TEXT, zipcode TEXT, PRIMARY KEY (id))"
        );
    }

    private void createOffsetTable() {
        cassandraSession.execute("CREATE TABLE IF NOT EXISTS customers.offset (tag TEXT, time_uuid TIMEUUID, PRIMARY KEY (tag))");
    }

    private Offset getOffset(String tag) {
        PreparedStatement ps = cassandraSession.prepare("SELECT time_uuid FROM customers.offset where tag = ?");
        BoundStatement bindGetOffset = ps.bind();
        bindGetOffset.setString("tag", tag);

        ResultSet rs = cassandraSession.execute(bindGetOffset);

        if(rs.iterator().hasNext()) {
            return TimeBasedUUID.timeBasedUUID(rs.one().getUUID("time_uuid"));
        }
        else {
            return TimeBasedUUID.noOffset();
        }
    }

    private void updateOffset(String tag, UUID timeUUID) {
        PreparedStatement ps = cassandraSession.prepare("INSERT INTO customers.offset (tag, time_uuid) VALUES (?, ?)");

        BoundStatement bindUpdateOffset = ps.bind();
        bindUpdateOffset.setString("tag", tag);
        bindUpdateOffset.setUUID("time_uuid", timeUUID);

        cassandraSession.execute(bindUpdateOffset);
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
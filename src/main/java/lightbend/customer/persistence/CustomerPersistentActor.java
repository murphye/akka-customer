package lightbend.customer.persistence;

import akka.actor.ActorSystem;
import akka.cluster.sharding.ShardRegion;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;

public class CustomerPersistentActor extends AbstractPersistentActor {

    private CustomerState state;
    private ActorSystem actorSystem;

    public CustomerPersistentActor() {
        this.actorSystem = this.getContext().getSystem();
    }

    @Override
    public String persistenceId() {
        return self().path().parent().name() + "-" + self().path().name();
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(SnapshotOffer.class, ss -> {
                    state = (CustomerState) ss.snapshot();
                    if(state.getStatus() == CustomerStatus.ADDED) {
                        getContext().become(becomeCustomerAddedState());
                    }
                    else if(state.getStatus() == CustomerStatus.DISABLED) {
                        getContext().become(becomeCustomerDisabledState());
                    }
                    else {
                        getContext().become(initialState());
                    }
                })
                .build();
    }

    @Override
    public Receive createReceive() {
        return initialState();
    }

    private Receive initialState() {
        return receiveBuilder()
                .match(CustomerCommand.AddCustomer.class, ac -> {
                        final CustomerEvent.CustomerAdded customerAdded = new CustomerEvent.CustomerAdded(ac.getCustomer());
                        persist(customerAdded, (CustomerEvent.CustomerAdded ca) -> {
                                CustomerState newState = CustomerState.addedCustomer(customerAdded.getCustomer());
                                saveSnapshot(newState);
                                this.state = newState;
                                getContext().become(becomeCustomerAddedState());

                        });
                })
                .match(CustomerCommand.GetCustomer.class, gc -> {
                    getSender().tell(state.getCustomer(), self());
                })
                .build();
    }

    private Receive becomeCustomerAddedState() {
        return receiveBuilder()
                .match(CustomerCommand.GetCustomer.class, gc -> {
                    getSender().tell(state.getCustomer(), self());
                })
                .match(CustomerCommand.DisableCustomer.class, dc -> {
                        final CustomerEvent.CustomerDisabled customerDisabled = new CustomerEvent.CustomerDisabled(this.state.getCustomer());
                        persist(customerDisabled, (CustomerEvent.CustomerDisabled cd) -> {
                            CustomerState newState = CustomerState.disabledCustomer(this.state.getCustomer());
                            saveSnapshot(newState);
                            this.state = newState;
                            getContext().become(becomeCustomerDisabledState());
                        });
                })
                .build();
    }

    private Receive becomeCustomerDisabledState() {
        return receiveBuilder()
        // No current behavior for disabled customers
        .build();
    }

    public static ShardRegion.MessageExtractor shardExtractor() {
        return new CustomerPersistentActorShardMessageExtractor();
    }

    private static class CustomerPersistentActorShardMessageExtractor extends ShardRegion.HashCodeMessageExtractor {
        CustomerPersistentActorShardMessageExtractor() {
            super(100);
        }

        @Override
        public String entityId(Object c) {
            if (c instanceof CustomerCommand) {
                return ((CustomerCommand) c).getCustomerId();
            }
            return null;
        }
    }
}
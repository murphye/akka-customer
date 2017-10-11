package lightbend.customer.persistence;

import lightbend.customer.api.Customer;
import lombok.Value;
import lombok.NonNull;

import java.io.Serializable;

public interface CustomerEvent extends Serializable {

    String TAG = CustomerEvent.class.getName();

    @Value
    final class CustomerAdded implements CustomerEvent {
        @NonNull private final Customer customer;
    }

    @Value
    final class CustomerDisabled implements CustomerEvent {
        @NonNull private final Customer customer;
    }
}


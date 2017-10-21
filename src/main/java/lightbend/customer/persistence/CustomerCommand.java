package lightbend.customer.persistence;

import lightbend.customer.api.Customer;
import lombok.Value;
import lombok.NonNull;
import java.io.Serializable;

public interface CustomerCommand extends Serializable {

    public String getCustomerId(); // Used for Cluster Sharding

    @Value
    final class AddCustomer implements CustomerCommand {
        @NonNull private final Customer customer;

        public String getCustomerId() {
            return customer.getId();
        }
    }

    @Value
    final class GetCustomer implements CustomerCommand {
        @NonNull private final String customerId;
    }

    @Value
    final class DisableCustomer implements CustomerCommand {
        @NonNull private final String customerId;
    }
}


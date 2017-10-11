package lightbend.customer.persistence;

import lightbend.customer.api.Customer;
import lombok.Value;
import lombok.NonNull;
import java.io.Serializable;

public interface CustomerCommand extends Serializable {

    @Value
    final class AddCustomer implements CustomerCommand {
        @NonNull private final Customer customer;
    }

    enum GetCustomer implements CustomerCommand {
        INSTANCE
    }

    enum DisableCustomer implements CustomerCommand {
        INSTANCE
    }
}


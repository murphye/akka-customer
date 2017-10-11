package lightbend.customer.persistence;

import lightbend.customer.api.Customer;
import lombok.Value;

import java.io.Serializable;

/**
 * Customer state object used for event sourcing.
 */
@Value
public class CustomerState implements Serializable {

    private final Customer customer;
    private final CustomerStatus status;

    public static CustomerState newCustomer() {
        return new CustomerState(null, CustomerStatus.NEW);
    }

    public static CustomerState addedCustomer(Customer customer) {
        return new CustomerState(customer, CustomerStatus.ADDED);
    }

    public static CustomerState disabledCustomer(Customer customer) {
        return new CustomerState(customer, CustomerStatus.DISABLED);
    }
}

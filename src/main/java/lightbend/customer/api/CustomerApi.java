package lightbend.customer.api;

import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.PathMatchers;
import akka.http.javadsl.server.Route;
import lightbend.customer.service.CustomerService;

import static akka.http.javadsl.server.Directives.*;

public class CustomerApi {

    private CustomerService customerService;

    public CustomerApi(CustomerService customerService) {
        this.customerService = customerService;
    }

    public Route customer() {
        return route(
                pathPrefix("customer", () -> route(
                        disableCustomer(),
                        getCustomer(),
                        pathEnd(() -> route(getCustomers(), postCustomer()))
        )));
    }

    Route getCustomers() {
        return get(() ->
                customerService.getCustomers()
        );
    }

    Route postCustomer() {
        return post(() ->
                entity(Jackson.unmarshaller(Customer.class), theCustomer ->
                        customerService.addCustomer(theCustomer)
                )
        );
    }

    Route getCustomer() {
        return path(PathMatchers.segment(), customerId ->
                customerService.getCustomer(customerId)
        );
    }

    Route disableCustomer() {
        return pathPrefix("disable", () ->
                path(PathMatchers.segment(), customerId ->
                        post(() -> customerService.disableCustomer(customerId))
        ));
    }
}

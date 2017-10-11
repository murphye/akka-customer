package lightbend.customer;

import akka.actor.ActorSystem;
import akka.http.javadsl.server.HttpApp;
import akka.http.javadsl.server.Route;
import lightbend.customer.api.CustomerApi;
import lightbend.customer.service.CustomerService;

/**
 */
public class CustomerApp extends HttpApp {

    private ActorSystem actorSystem;

    private CustomerService customerService;

    private CustomerApi customerApi;

    public CustomerApp(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
        this.customerService = new CustomerService(actorSystem);
        this.customerApi = new CustomerApi(this.customerService);
    }

    @Override
    protected Route routes() {
        return route(customerApi.customer());
    }

    public static void main(String[] args) throws Exception {
        final ActorSystem system = ActorSystem.apply("customer");
        final CustomerApp myServer = new CustomerApp(system);
        // This will start the server until the return key is pressed
        myServer.startServer("localhost", 8080, system);
    }
}


package lightbend.customer;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.server.HttpApp;
import akka.http.javadsl.server.Route;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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

    public static void startup(String[] ports) throws Exception {
        for (String port : ports) {
            // Override the configuration of the port
            Config config = ConfigFactory.parseString(
                    "akka.remote.netty.tcp.port=" + port).withFallback(
                    ConfigFactory.load());

            // Create an Akka system
            ActorSystem system = ActorSystem.create("customer", config);

            final CustomerApp myServer = new CustomerApp(system);
            // This will start the server until the return key is pressed
            myServer.startServer("localhost", 9000, system);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            startup(new String[] { "2551", "0" });
        else
            startup(args);
    }
}


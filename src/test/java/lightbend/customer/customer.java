package lightbend.customer;

import akka.actor.ActorSystem;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRoute;
import akka.persistence.cassandra.testkit.CassandraLauncher;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.*;

import java.io.File;
import java.util.UUID;

/**
 * TODO: This test fails without being named "customer" and has something to do with the fact that the actor system
 * is derived from this class name.
 */
public class customer extends JUnitRouteTest {

    Config config = ConfigFactory.defaultApplication().resolve();

    static ActorSystem system;
    static CustomerApp customerApp;
    static TestRoute appRoute;
    static Route routes;

    @BeforeClass
    public static void setup() throws Exception {
        File cassandraDirectory = new File("target/customer");
        CassandraLauncher.start(cassandraDirectory, CassandraLauncher.DefaultTestConfigResource(), true, 19042);
    }

    @AfterClass
    public static void tearDown() {
        CassandraLauncher.stop();
    }

    @Test
    public void testCustomer() throws Exception {

        CustomerApp customerApp = new CustomerApp(super.system());
        TestRoute appRoute = testRoute(customerApp.routes());

        String uuid = UUID.randomUUID().toString();

        appRoute.run(HttpRequest.POST("/customer").
                withEntity(ContentTypes.APPLICATION_JSON,"{\"id\": \"" + uuid + "\", \"name\": \"Eric Murphy\", \"city\": \"San Francisco\", \"state\": \"CA\", \"zipCode\": \"94105\"}")

        ).assertStatusCode(202);

        Thread.sleep(5000);

        appRoute.run(HttpRequest.GET("/customer/" + uuid))
                .assertStatusCode(200)
                .assertEntity("{\"city\":\"San Francisco\",\"id\":\"" + uuid + "\",\"name\":\"Eric Murphy\",\"state\":\"CA\",\"zipCode\":\"94105\"}");


        appRoute.run(HttpRequest.PUT("/customer/disable/" + uuid))
                .assertStatusCode(202)
                .assertEntity("Customer disabled");
    }
}
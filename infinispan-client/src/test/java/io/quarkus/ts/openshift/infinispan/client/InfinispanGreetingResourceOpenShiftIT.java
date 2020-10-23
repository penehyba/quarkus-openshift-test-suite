package io.quarkus.ts.openshift.infinispan.client;

import io.quarkus.ts.openshift.common.AdditionalResources;
import io.quarkus.ts.openshift.common.OpenShiftTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@OpenShiftTest
@AdditionalResources("classpath:test-cache.yaml")
public class InfinispanGreetingResourceOpenShiftIT {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/infinispan")
                .then()
                .statusCode(200)
                .body(is("Hello World, Infinispan is up!"));
    }
}

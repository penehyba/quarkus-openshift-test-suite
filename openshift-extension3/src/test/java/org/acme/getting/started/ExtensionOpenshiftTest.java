package org.acme.getting.started;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;

@QuarkusTest
public class ExtensionOpenshiftTest {
    private static final String APP = "getting-started";

    private static final String PATH_APP = "./";// + APP + "/";

    private static final String PATH_APP_RESOURCE2CHANGE = PATH_APP + "src/main/java/org/acme/getting/started/GreetResource.java";

    private static final String PATH_APP_POM = PATH_APP + "pom.xml";

    private static final String PATH_APP_PROPERTIES = PATH_APP + "src/main/resources/application.properties";

    private static final String MVNW = PATH_APP + "mvnw";

    private List<String> properties = Collections.unmodifiableList(
            Arrays.asList(
                    "quarkus.kubernetes-client.trust-certs=true",
                    "quarkus.s2i.base-jvm-image=registry.access.redhat.com/openjdk/openjdk-11-rhel7",
                    "quarkus.openshift.expose=true"
            ));

    @Test
    public void checkAddExtension_QuarkusOpenshift() throws IOException, InterruptedException {
        new MyCommand("pwd").runAndWait();
//        new MyCommand("mkdir", "./generated").runAndWait();
//        new MyCommand("cd", "./generated").runAndWait();
        new MyCommand("ls").runAndWait();
//        new MyCommand("cp", "./mvnw", "./generated/").runAndWait();
//        new MyCommand("cp", "./mvnw.cmd", "./generated/").runAndWait();
//        new MyCommand("pwd").runAndWait();
//        new MyCommand(MVNW, "clean", "io.quarkus:quarkus-maven-plugin:1.4.1.Final:create",
//                      "-DprojectGroupId=org.acme",
//                      "-DprojectArtifactId=getting-started",
//                      "-DclassName=\"org.acme.getting.started.GreetingResource\"",
//                      "-Dpath=\"/hello\"",
//                      "-Dbasedir=\"../generated\"").runAndWait();
//        new MyCommand("cd", "getting-started").runAndWait();
//        new MyCommand("./mvnw", "clean", "compile", "quarkus:dev").runAndWait();
//        new MyCommand("curl", "-w", "\"\\n\"", "http://localhost:8080/hello").runAndWait();
//        when()
//                .get("hello")
//                .then()
//                .statusCode(200)
//                .body(equalTo("hello"));

        // pom has no 'quarkus-openshift' dependency, application.properties does not exist and the app is 'getting-started'
        Assertions.assertFalse(fileContainsKey(PATH_APP_POM, "quarkus-openshift"));
        Assertions.assertFalse(new File(PATH_APP_PROPERTIES).exists());
//        Assertions.assertTrue(fileContainsKey(PATH_APP_POM, "<artifactId>" + APP + "</artifactId>"));
//
        final String projectName = ThreadLocalRandom.current()
                .ints(10, 'a', 'z' + 1)
                .collect(() -> new StringBuilder("tss-"), StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        new MyCommand("oc", "new-project", projectName).runAndWait(); // we can't use infrastructure enabled by @OpenShiftTest
        final OpenShiftClientSource openShiftClientResource = OpenShiftClientSource.createDefault();
        final OpenShiftClient oc = openShiftClientResource.client;
        // apply 'quarkus-openshift' extension and create application.properties
        new MyCommand(MVNW, "quarkus:add-extension",
                      "-Dversion.quarkus=" + System.getProperty("version.quarkus"),
                      "-Dextensions=openshift", "-f", PATH_APP_POM).runAndWait();
        createApplicationProperties(PATH_APP_PROPERTIES, properties);
        Assertions.assertTrue(fileContainsKey(PATH_APP_POM, "quarkus-openshift"));
        Assertions.assertTrue(new File(PATH_APP_PROPERTIES).exists());

        // run app on OpenShift and check address
        runAppAndVerify(true, oc, projectName);
        Assertions.assertEquals(oc.deploymentConfigs().list().getItems().stream().findFirst().get().getMetadata().getName(), APP,
                                "Deployment config name should equal '" + APP + "'.");
        Assertions.assertEquals(oc.services().list().getItems().stream().findFirst().get().getMetadata().getName(), APP,
                                "Service name should equal '" + APP + "'.");
        Assertions.assertEquals(oc.routes().list().getItems().stream().findFirst().get().getMetadata().getName(), APP,
                                "Route name should equal '" + APP + "'.");

        // redeploy the app and verify modifications
        modifyFileForTesting(PATH_APP_RESOURCE2CHANGE);
        runAppAndVerify(false, oc, projectName);

//        new MyCommand("oc", "delete", "project", projectName).runAndWait();
    }

    private void runAppAndVerify(boolean before, OpenShiftClient oc, String projectName) throws
            IOException, InterruptedException {
        System.out.println(before ? "BEFORE STARTS" : "AFTER STARTS");
        new MyCommand(MVNW, "clean", "dependency:tree", "package",
//                      "-f", PATH_APP_POM,
                      "-Dquarkus.kubernetes.deploy=true",
                      "-Dquarkus.openshift.deploy=true",
                      "-Dversion.quarkus=" + System.getProperty("version.quarkus"),
                      "-DskipTests",
                      "-DskipITs").runAndWait();

        Assertions.assertEquals(projectName, oc.getNamespace());
        new MyCommand("sleep", "40s").runAndWait();
        String url = "http://" + oc.routes().withName(APP).get().getSpec().getHost() + "/hello";
        new MyCommand("curl", url).runAndWait();
        await().atMost(5, TimeUnit.MINUTES).untilAsserted(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    final String expected = before ? "hello before" : "hello after";
                    Assertions.assertEquals(expected,
                                            line,
                                            "Response should contain one row message '" + expected + "'");
                }
            }
//            given() // TODO feel free to make this work
//                    when()
//                    .get(url)
//                    .then()
//                    .statusCode(200)
//                    .body(equalTo(before ? "hello before" : "hello after"));
        });
        System.out.println(before ? "BEFORE ENDS" : "AFTER ENDS");
    }

    private void modifyFileForTesting(String path) throws IOException {
        try (Stream<String> lines = Files.lines(Paths.get(path))) {
            List<String> replaced = lines
                    .map(line -> line.replaceAll("hello before", "hello after"))
                    .collect(Collectors.toList());
            Files.write(Paths.get(path), replaced);
        }
    }

    private void createApplicationProperties(String propPath, List<String> properties) throws IOException {
        FileWriter fw = new FileWriter(propPath, true);
        BufferedWriter bw = new BufferedWriter(fw);
        for (String property : properties) {
            bw.write(property);
            bw.newLine();
        }
        bw.close();
    }

    private boolean fileContainsKey(String path, String key) throws IOException {
        return Files.lines(Paths.get(path)).anyMatch(line -> line.contains(key));
    }
}

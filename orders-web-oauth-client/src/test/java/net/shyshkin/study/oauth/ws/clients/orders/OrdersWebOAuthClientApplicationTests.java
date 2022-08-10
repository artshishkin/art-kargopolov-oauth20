package net.shyshkin.study.oauth.ws.clients.orders;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.function.Consumer;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("local")
class OrdersWebOAuthClientApplicationTests {

    private static final int hostPort = 8000;
    private static final int containerExposedPort = 8080;
    private static final Consumer<CreateContainerCmd> cmd = e -> e.withPortBindings(new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(containerExposedPort)));

    @Container
    static GenericContainer<?> newSpringAuthServer = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-new-spring-authorization-server")
            .withNetworkAliases("new-spring-auth-server")
            .withEnv("app.auth-server.provider.issuer", "http://localhost:8000")
            .withExposedPorts(8080)
            .withCreateContainerCmdModifier(cmd)
            .waitingFor(Wait.forHealthcheck());

    @Test
    void contextLoads() {
    }
}
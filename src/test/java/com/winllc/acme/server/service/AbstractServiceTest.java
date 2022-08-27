package com.winllc.acme.server.service;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.winllc.acme.server.TestConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AbstractServiceTest {

    public static GenericContainer mongoDBContainer = new GenericContainer<>(DockerImageName.parse("mongo:4.0.10")
            .asCompatibleSubstituteFor("mongo"))
            .withExposedPorts(27017)
            .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                    new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(27017), new ExposedPort(27017)))
            ))
            .withEnv("MONGO_INITDB_DATABASE", "acme")
            //.withCommand("--replSet", "docker-rs")
            .waitingFor(Wait.forLogMessage("(?i).*waiting for connections.*", 1));

    @BeforeAll
    static void beforeAll() throws Exception {
        mongoDBContainer.start();
    }

    @AfterAll
    static void afterAll(){
        mongoDBContainer.stop();
    }

}

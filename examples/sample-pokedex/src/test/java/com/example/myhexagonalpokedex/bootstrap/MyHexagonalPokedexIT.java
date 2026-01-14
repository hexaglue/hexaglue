package com.example.myhexagonalpokedex.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.sun.istack.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = MyHexagonalPokedexApplicationIT.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(
        initializers = {
                MyHexagonalPokedexIT.DockerPostgresDataSourceInitializer.class,
                MyHexagonalPokedexIT.WireMockInitializer.class
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class MyHexagonalPokedexIT {

    @Autowired
    protected WireMockServer wireMockServer;

    @Autowired
    protected ObjectMapper objectMapper;

    public static PostgreSQLContainer<?> postgreDBContainer = new PostgreSQLContainer<>(
            "postgres:16.1-alpine"
    );

    static {
        postgreDBContainer.start();
    }

    public static class DockerPostgresDataSourceInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(
                @NotNull ConfigurableApplicationContext applicationContext
        ) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    "spring.datasource.url=" +
                            postgreDBContainer.getJdbcUrl(),
                    "spring.datasource.username=" +
                            postgreDBContainer.getUsername(),
                    "spring.datasource.password=" +
                            postgreDBContainer.getPassword()
            );
        }
    }

    public static class WireMockInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            WireMockServer wiremockServer = new WireMockServer(new WireMockConfiguration().dynamicPort());
            wiremockServer.start();

            configurableApplicationContext
                    .getBeanFactory()
                    .registerSingleton("wiremockServer", wiremockServer);

            configurableApplicationContext.addApplicationListener(
                    applicationEvent -> {
                        if (applicationEvent instanceof ContextClosedEvent) {
                            wiremockServer.stop();
                        }
                    });
            TestPropertyValues.of("infrastructure.pokeapi.app.api:http://localhost:" + wiremockServer.port())
                    .applyTo(configurableApplicationContext);
        }

    }
}

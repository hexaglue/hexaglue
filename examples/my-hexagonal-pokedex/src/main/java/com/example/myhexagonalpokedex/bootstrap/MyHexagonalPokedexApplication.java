package com.example.myhexagonalpokedex.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.myhexagonalpokedex")
@EntityScan(basePackages = {
        "com.example.myhexagonalpokedex.bootstrap.infrastructure.persistence"
})
@EnableJpaRepositories(basePackages = {
        "com.example.myhexagonalpokedex.bootstrap.infrastructure.persistence"
})
public class MyHexagonalPokedexApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyHexagonalPokedexApplication.class, args);
    }
}

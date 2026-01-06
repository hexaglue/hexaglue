package com.example.myhexagonalpokedex.bootstrap;

import com.example.myhexagonalpokedex.domain.pokemon.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfiguration {

    @Bean
    public CapturablePokemonUseCase capturablePokemonUseCase(PokemonApiFetcher pokemonApiFetcher,
                                                             PokemonRepositoryFetcher pokemonRepositoryFetcher) {
        return new CapturablePokemonUseCase(pokemonApiFetcher, pokemonRepositoryFetcher);
    }

    @Bean
    public CapturePokemonUseCase capturePokemonUseCase(PokemonApiFetcher pokemonApiFetcher,
                                                       PokemonRepositoryFetcher pokemonRepositoryFetcher,
                                                       PokemonRepositorySaver pokemonRepositorySaver) {
        return new CapturePokemonUseCase(pokemonApiFetcher, pokemonRepositoryFetcher, pokemonRepositorySaver);
    }
}

package com.example.myhexagonalpokedex.infrastructure.pokeapi;

import com.example.myhexagonalpokedex.domain.pokemon.CapturablePokemon;
import com.example.myhexagonalpokedex.domain.pokemon.Pokemon;
import com.example.myhexagonalpokedex.domain.pokemon.PokemonApiFetcher;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PokeApiAdapter implements PokemonApiFetcher {

    private final RestClient restClient;

    public PokeApiAdapter(@Value("${pokeapi.base-url:https://pokeapi.co/api/v2}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public List<CapturablePokemon> findTopTwenty() {
        PokemonListResponse response = restClient.get()
                .uri("/pokemon?limit=20")
                .retrieve()
                .body(PokemonListResponse.class);

        if (response == null || response.results() == null) {
            return List.of();
        }

        return response.results().stream()
                .map(r -> new CapturablePokemon(extractId(r.url()), r.name()))
                .toList();
    }

    @Override
    public Pokemon findById(Integer pokemonId) {
        PokemonDetailResponse response = restClient.get()
                .uri("/pokemon/{id}", pokemonId)
                .retrieve()
                .body(PokemonDetailResponse.class);

        if (response == null) {
            return null;
        }

        String ability = response.abilities() != null && !response.abilities().isEmpty()
                ? response.abilities().get(0).ability().name()
                : "unknown";

        return new Pokemon(response.id(), response.name(), ability);
    }

    private Integer extractId(String url) {
        String[] parts = url.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    record PokemonListResponse(List<PokemonRef> results) {}
    record PokemonRef(String name, String url) {}
    record PokemonDetailResponse(Integer id, String name, List<AbilityWrapper> abilities) {}
    record AbilityWrapper(AbilityRef ability) {}
    record AbilityRef(String name) {}
}

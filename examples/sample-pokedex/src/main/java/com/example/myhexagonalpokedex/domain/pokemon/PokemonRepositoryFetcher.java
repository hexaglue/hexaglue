package com.example.myhexagonalpokedex.domain.pokemon;

import java.util.List;
import java.util.Optional;

public interface PokemonRepositoryFetcher {
    List<Pokemon> findAll();

    Optional<Pokemon> findById(Integer id);
}

package com.example.myhexagonalpokedex.domain.pokemon;

import java.util.List;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface PokemonApiFetcher {
    List<CapturablePokemon> findTopTwenty();

    Pokemon findById(Integer pokemonId);
}

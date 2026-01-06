package com.example.myhexagonalpokedex.domain.pokemon;

import com.example.myhexagonalpokedex.core.exception.ExceptionCode;
import com.example.myhexagonalpokedex.core.exception.MyHexagonalPokedexException;
import java.util.Optional;

public class CapturePokemonUseCase {
    private final PokemonApiFetcher pokemonApiFetcher;
    private final PokemonRepositoryFetcher pokemonRepositoryFetcher;
    private final PokemonRepositorySaver pokemonRepositorySaver;

    public CapturePokemonUseCase(PokemonApiFetcher pokemonApiFetcher, PokemonRepositoryFetcher pokemonRepositoryFetcher, PokemonRepositorySaver pokemonRepositorySaver) {
        this.pokemonApiFetcher = pokemonApiFetcher;
        this.pokemonRepositoryFetcher = pokemonRepositoryFetcher;
        this.pokemonRepositorySaver = pokemonRepositorySaver;
    }

    public void capture(Integer pokemonId) {
        final Optional<Pokemon> alreadySavedPokemon = pokemonRepositoryFetcher.findById(pokemonId);
        if (alreadySavedPokemon.isPresent()) {
            final String message = String.format("Pokemon with id '%s' has already been captured", pokemonId);
            throw new MyHexagonalPokedexException(ExceptionCode.ALREADY_CAPTURED_POKEMON, message);
        }
        final Pokemon pokemonToCapture = pokemonApiFetcher.findById(pokemonId);
        pokemonRepositorySaver.save(pokemonToCapture);
    }
}

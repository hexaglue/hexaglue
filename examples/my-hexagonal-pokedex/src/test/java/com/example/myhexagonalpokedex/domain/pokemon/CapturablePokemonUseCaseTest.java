package com.example.myhexagonalpokedex.domain.pokemon;

import static org.mockito.Mockito.when;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CapturablePokemonUseCaseTest {

    @Mock
    PokemonApiFetcher pokemonApiFetcher;

    @Mock
    PokemonRepositoryFetcher pokemonRepositoryFetcher;

    @InjectMocks
    CapturablePokemonUseCase capturablePokemonUseCase;


    @Test
    void should_return_capturable_pokemons() {
        // Given
        final List<Pokemon> mockOwnedPokemonList = List.of(
                new Pokemon(1, "pokemon-1", "ability-1"),
                new Pokemon(4, "pokemon-4", "ability-4")
        );
        final List<CapturablePokemon> mockedCapturablePokemons = List.of(
                new CapturablePokemon(1, "pokemon-1"),
                new CapturablePokemon(2, "pokemon-2"),
                new CapturablePokemon(3, "pokemon-3"),
                new CapturablePokemon(4, "pokemon-4")
        );
        when(pokemonRepositoryFetcher.findAll()).thenReturn(mockOwnedPokemonList);
        when(pokemonApiFetcher.findTopTwenty()).thenReturn(mockedCapturablePokemons);

        // When
        final List<CapturablePokemon> capturablePokemons = capturablePokemonUseCase.findAllInTopTwenty();

        // Then
        Assertions.assertThat(capturablePokemons)
                .hasSize(2)
                .containsExactlyInAnyOrder(
                        new CapturablePokemon(
                                2,
                                "pokemon-2"
                        ),
                        new CapturablePokemon(
                                3,
                                "pokemon-3"
                        )
                );
    }
}

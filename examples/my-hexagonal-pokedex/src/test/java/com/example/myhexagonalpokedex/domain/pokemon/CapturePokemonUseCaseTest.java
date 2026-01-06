package com.example.myhexagonalpokedex.domain.pokemon;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.myhexagonalpokedex.core.exception.MyHexagonalPokedexException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CapturePokemonUseCaseTest {
    @Mock
    PokemonRepositoryFetcher pokemonRepositoryFetcher;
    @Mock
    PokemonApiFetcher pokemonApiFetcher;
    @Mock
    PokemonRepositorySaver pokemonRepositorySaver;
    @InjectMocks
    CapturePokemonUseCase capturePokemonUseCase;

    @Test
    void should_save_pokemon() {
        // Given
        final Integer pokemonId = 1;
        final Pokemon mockedPokemonToSave = new Pokemon(pokemonId, "name", "ability");
        when(pokemonRepositoryFetcher.findById(pokemonId)).thenReturn(Optional.empty());
        when(pokemonApiFetcher.findById(pokemonId)).thenReturn(mockedPokemonToSave);

        // When
        capturePokemonUseCase.capture(pokemonId);

        // Then
        verify(pokemonRepositorySaver, times(1)).save(mockedPokemonToSave);
    }

    @Test
    void should_throw_when_pokemon_is_already_saved() {
        // Given
        final Integer pokemonId = 1;
        when(pokemonRepositoryFetcher.findById(pokemonId))
                .thenReturn(Optional.of(new Pokemon(pokemonId, "name", "ability")));

        // When
        // Then
        assertThatThrownBy(() -> capturePokemonUseCase.capture(pokemonId))
                .isExactlyInstanceOf(MyHexagonalPokedexException.class)
                .hasMessage("Pokemon with id '1' has already been captured");
    }
}

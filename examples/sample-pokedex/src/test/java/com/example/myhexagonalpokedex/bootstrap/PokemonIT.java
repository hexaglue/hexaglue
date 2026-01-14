package com.example.myhexagonalpokedex.bootstrap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.myhexagonalpokedex.bootstrap.infrastructure.persistence.PokemonEntity;
import com.example.myhexagonalpokedex.bootstrap.infrastructure.persistence.PokemonJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class PokemonIT extends MyHexagonalPokedexIT {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    PokemonJpaRepository pokemonRepository;

    @AfterEach
    public void tearDown() {
        pokemonRepository.deleteAll();
    }

    @Test
    void should_retrieve_capturable_pokemons_within_top_twenty_without_owned_pokemon() throws Exception {
        // Given
        wireMockServer.stubFor(
                get(
                        urlPathEqualTo("/pokemon"))
                        .willReturn(aResponse().withBodyFile("pokeapi_stubs/get_top_twenty_pokemons.json")));
        // When
        mockMvc
                .perform(MockMvcRequestBuilders.get("/pokemons/capturable"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("bulbasaur"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("ivysaur"))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].name").value("venusaur"));
    }

    @Test
    void should_retrieve_capturable_pokemons_within_top_twenty_with_already_owned_pokemons() throws Exception {
        // Given
        final PokemonEntity ownedPokemon = createPokemonEntity(2, "ivysaur", "overgrow");
        pokemonRepository.save(ownedPokemon);

        wireMockServer.stubFor(
                get(
                        urlPathEqualTo("/pokemon"))
                        .willReturn(aResponse().withBodyFile("pokeapi_stubs/get_top_twenty_pokemons.json")));
        // When
        mockMvc
                .perform(MockMvcRequestBuilders.get("/pokemons/capturable"))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("bulbasaur"))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[1].name").value("venusaur"));
    }

    @Test
    void should_save_new_capturable_pokemon() throws Exception {
        // Given
        wireMockServer.stubFor(
                get(
                        urlPathEqualTo("/pokemon/2"))
                        .willReturn(aResponse().withBodyFile("pokeapi_stubs/get_pokemon_details.json")));

        // When
        mockMvc
                .perform(MockMvcRequestBuilders.post("/pokemons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                 "id": 2
                                }
                                 """)
                )
                // Then
                .andExpect(status().isOk());

        final Optional<PokemonEntity> savedPokemonEntity = pokemonRepository.findById(2);
        assertThat(savedPokemonEntity).isPresent();
        assertThat(savedPokemonEntity.get().getId()).isEqualTo(2);
        assertThat(savedPokemonEntity.get().getName()).hasToString("ivysaur");
        assertThat(savedPokemonEntity.get().getAbility()).isEqualTo("overgrow");
    }

    @Test
    void should_throw_trying_to_save_already_owned_pokemon() throws Exception {
        // When
        final PokemonEntity ownedPokemon = createPokemonEntity(2, "ivysaur", "overgrow");
        pokemonRepository.save(ownedPokemon);
        wireMockServer.stubFor(
                get(
                        urlPathEqualTo("/pokemon/2"))
                        .willReturn(aResponse().withBodyFile("pokeapi_stubs/get_pokemon_details.json")));
        // When
        mockMvc
                .perform(MockMvcRequestBuilders.post("/pokemons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                 "id": 2
                                }
                                 """)
                )
                // Then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$[0].exceptionCode").value("ALREADY_CAPTURED_POKEMON"))
                .andExpect(jsonPath("$[0].technicalMessage").value("Pokemon with id '2' has already been captured"));
    }

    private PokemonEntity createPokemonEntity(Integer id, String name, String ability) {
        PokemonEntity entity = new PokemonEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setAbility(ability);
        return entity;
    }
}

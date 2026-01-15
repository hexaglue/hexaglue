package com.example.myhexagonalpokedex.infrastructure.rest;

import com.example.myhexagonalpokedex.domain.pokemon.CapturablePokemon;
import com.example.myhexagonalpokedex.domain.pokemon.CapturablePokemonUseCase;
import com.example.myhexagonalpokedex.domain.pokemon.CapturePokemonUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pokemons")
public class PokemonController {

    private final CapturablePokemonUseCase capturablePokemonUseCase;
    private final CapturePokemonUseCase capturePokemonUseCase;

    public PokemonController(CapturablePokemonUseCase capturablePokemonUseCase,
                             CapturePokemonUseCase capturePokemonUseCase) {
        this.capturablePokemonUseCase = capturablePokemonUseCase;
        this.capturePokemonUseCase = capturePokemonUseCase;
    }

    @GetMapping("/capturable")
    public List<CapturablePokemon> getCapturablePokemons() {
        return capturablePokemonUseCase.findAllInTopTwenty();
    }

    @PostMapping
    public void capturePokemon(@RequestBody CapturePokemonRequest request) {
        capturePokemonUseCase.capture(request.id());
    }

    public record CapturePokemonRequest(Integer id) {}
}

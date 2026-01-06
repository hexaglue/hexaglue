package com.example.myhexagonalpokedex.core.exception;

public enum ExceptionCode {
    ERROR_WHILE_EXECUTING_HTTP_REQUEST(500),
    POKEAPI_ERROR(500),
    POSTGRES_FIND_POKEMON_ERROR(500),
    POSTGRES_SAVE_POKEMON_ERROR(500),
    ALREADY_CAPTURED_POKEMON(400);

    private final Integer httpStatusCode;

    ExceptionCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
}

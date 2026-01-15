package com.example.myhexagonalpokedex.core.exception;

public class MyHexagonalPokedexException extends RuntimeException {
    private final ExceptionCode exceptionCode;

    public MyHexagonalPokedexException(ExceptionCode exceptionCode, String message, Throwable throwable) {
        super(message, throwable);
        this.exceptionCode = exceptionCode;
    }

    public MyHexagonalPokedexException(ExceptionCode exceptionCode, String message) {
        super(message);
        this.exceptionCode = exceptionCode;
    }

    public ExceptionCode getExceptionCode() {
        return exceptionCode;
    }
}

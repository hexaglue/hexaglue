package com.example.myhexagonalpokedex.infrastructure.rest;

import com.example.myhexagonalpokedex.core.exception.MyHexagonalPokedexException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MyHexagonalPokedexException.class)
    public ResponseEntity<List<ErrorResponse>> handleMyHexagonalPokedexException(MyHexagonalPokedexException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getExceptionCode().name(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(List.of(errorResponse));
    }

    public record ErrorResponse(String exceptionCode, String technicalMessage) {}
}

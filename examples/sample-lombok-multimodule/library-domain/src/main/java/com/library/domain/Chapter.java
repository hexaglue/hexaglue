package com.library.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Chapter entity within a book.
 *
 * <p>Uses {@code @RequiredArgsConstructor} — Lombok generates the all-final-fields constructor.
 * HexaGlue needs delombok to see this constructor for proper classification.
 *
 * @since 6.1.0
 */
@Getter
@RequiredArgsConstructor
public class Chapter {

    private final int number;
    private final String title;
    private final int pageCount;
}

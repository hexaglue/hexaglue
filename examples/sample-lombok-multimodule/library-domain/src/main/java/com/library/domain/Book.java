package com.library.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Book aggregate root.
 *
 * <p>Uses {@code @Getter} and {@code @NoArgsConstructor(access = PROTECTED)} — Lombok
 * constructor annotations that require delombok for HexaGlue to classify correctly.
 *
 * @since 6.1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    private BookId id;
    private String title;
    private String author;
    private Genre genre;
    private final List<Chapter> chapters = new ArrayList<>();

    public Book(BookId id, String title, String author, Genre genre) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
    }

    public List<Chapter> getChapters() {
        return Collections.unmodifiableList(chapters);
    }

    public void addChapter(String title, int pageCount) {
        int number = chapters.size() + 1;
        chapters.add(new Chapter(number, title, pageCount));
    }
}

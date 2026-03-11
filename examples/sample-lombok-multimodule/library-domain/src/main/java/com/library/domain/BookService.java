package com.library.domain;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Application service orchestrating book use cases.
 *
 * <p>Uses {@code @RequiredArgsConstructor} for constructor injection of dependencies —
 * the standard Spring pattern that requires delombok for HexaGlue to analyze.
 *
 * @since 6.1.0
 */
@RequiredArgsConstructor
public class BookService implements BrowsingBooks {

    private final BookRepository bookRepository;

    @Override
    public Book addBook(String title, String author, Genre genre) {
        Book book = new Book(BookId.generate(), title, author, genre);
        return bookRepository.save(book);
    }

    @Override
    public Optional<Book> findBook(BookId id) {
        return bookRepository.findById(id);
    }

    @Override
    public List<Book> listAllBooks() {
        return bookRepository.findAll();
    }
}

package com.library.domain;

import java.util.List;
import java.util.Optional;

/**
 * Driving port for book browsing use cases.
 *
 * @since 6.1.0
 */
public interface BrowsingBooks {

    Book addBook(String title, String author, Genre genre);

    Optional<Book> findBook(BookId id);

    List<Book> listAllBooks();
}

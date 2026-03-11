package com.library.domain;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Book aggregate persistence.
 *
 * @since 6.1.0
 */
public interface BookRepository {

    Book save(Book book);

    Optional<Book> findById(BookId id);

    List<Book> findAll();

    void delete(Book book);
}

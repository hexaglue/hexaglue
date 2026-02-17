package com.example.ports.out;

import com.example.domain.Book;
import com.example.domain.BookId;
import java.util.Optional;

/** Secondary port for book persistence. */
public interface BookRepository {
    Book save(Book book);
    Optional<Book> findById(BookId id);
}

package com.example.ports.in;

import com.example.domain.Book;
import com.example.domain.BookId;
import java.util.List;

/** Primary port for book operations. */
public interface BookUseCases {

    Book createBook(String title, String author);

    List<Book> listBooks();

    Book updateBook(BookId id, String title, String author);
}

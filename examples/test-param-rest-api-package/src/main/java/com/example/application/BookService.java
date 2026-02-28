package com.example.application;

import com.example.domain.Book;
import com.example.domain.BookId;
import com.example.ports.in.BookUseCases;
import java.util.List;

/** Application service implementing book use cases. */
public class BookService implements BookUseCases {

    @Override
    public Book createBook(String title, String author) {
        return new Book(BookId.generate(), title, author);
    }

    @Override
    public List<Book> listBooks() {
        return List.of();
    }

    @Override
    public Book updateBook(BookId id, String title, String author) {
        return new Book(id, title, author);
    }
}

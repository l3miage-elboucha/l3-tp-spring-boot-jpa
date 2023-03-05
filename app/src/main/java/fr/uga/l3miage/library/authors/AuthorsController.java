package fr.uga.l3miage.library.authors;

import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.library.books.BookDTO;
import fr.uga.l3miage.library.books.BooksMapper;
import fr.uga.l3miage.library.service.AuthorService;
import fr.uga.l3miage.library.service.BookService;
import fr.uga.l3miage.library.service.DeleteAuthorException;
import fr.uga.l3miage.library.service.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class AuthorsController {

    private final AuthorService authorService;
    private final AuthorMapper authorMapper;
    private final BookService bookService;
    private final BooksMapper booksMapper;

    @Autowired
    public AuthorsController(AuthorService authorService, AuthorMapper authorMapper, BooksMapper booksMapper, BookService bookService) {
        this.authorService = authorService;
        this.authorMapper = authorMapper;
        this.booksMapper = booksMapper;
        this.bookService = bookService;
    }

    @GetMapping("/authors")
    public Collection<AuthorDTO> authors(@RequestParam(value = "q", required = false) String query) {
        Collection<Author> authors;
        if (query == null) {
            authors = authorService.list();
        } else {
            authors = authorService.searchByName(query);
        }
        return authors.stream()
                .map(authorMapper::entityToDTO)
                .toList();
    }

    @GetMapping("/authors/{id}")
    public AuthorDTO author(@PathVariable("id") Long id) throws EntityNotFoundException {

        try {
            var author = this.authorService.get(id);
            if ( author == null ) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return authorMapper.entityToDTO(author);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorDTO newAuthor(@RequestBody AuthorDTO author) {

        /* conversion to Entity */
        Author authorEntity = authorMapper.dtoToEntity(author);

        /* check if the fullname is null or empty (fullname: " ") */
        String authorName = authorEntity.getFullName();
        if (authorName == null || authorName.trim().isEmpty()) {
            throw new ResponseStatusException((HttpStatus.BAD_REQUEST));
        }

        /* creation of the author */
        authorService.save(authorEntity);

        /* conversion back to DTO */
        return authorMapper.entityToDTO(authorEntity);

    }

    @PutMapping("/authors/{id}")
    public AuthorDTO updateAuthor(@RequestBody AuthorDTO author, @PathVariable("id") Long id) throws EntityNotFoundException {

        /* attention AuthorDTO.id() doit être égale à id, sinon la requête utilisateur est mauvaise */


        if (author.id() != id) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        /* Does the author exist ? */
        try {
            var authorExistence = this.authorService.get(id);
            if (authorExistence == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }

            /* Update */
            Author updatedAuthor = authorMapper.dtoToEntity(author);
            updatedAuthor = this.authorService.update(updatedAuthor);

            /* Conversion to DTO */
            return authorMapper.entityToDTO(updatedAuthor);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found");
        }
    }

    @DeleteMapping("/authors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthor(@PathVariable("id") Long id) throws EntityNotFoundException, DeleteAuthorException {
        // implemented
        try {
            var author = this.authorService.get(id);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Can't delete an author that doesn't exist");
        }
        try {
            for (Book book : bookService.getByAuthor(id)) {
                if (book.getAuthors().size() > 1) {
                    bookService.delete(book.getId());
                }
            }
            this.authorService.delete(id);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/authors/{id}/books")
    public Collection<BookDTO> books(@PathVariable("id") Long authorId) throws EntityNotFoundException {
        try {
            Collection<Book> books;
            var author = this.authorService.get(authorId);
            if (author == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found");
            }
            books = author.getBooks();
            return books.stream().map(booksMapper::entityToDTO).toList();
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found");
        }
    }
}

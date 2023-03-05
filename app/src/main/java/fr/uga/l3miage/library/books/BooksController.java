package fr.uga.l3miage.library.books;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.uga.l3miage.data.domain.Author;
import fr.uga.l3miage.data.domain.Book;
import fr.uga.l3miage.library.authors.AuthorDTO;
import fr.uga.l3miage.library.authors.AuthorMapper;
import fr.uga.l3miage.library.service.AuthorService;
import fr.uga.l3miage.library.service.BookService;
import fr.uga.l3miage.library.service.DeleteAuthorException;
import fr.uga.l3miage.library.service.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.apache.commons.lang3.EnumUtils;

import static fr.uga.l3miage.data.domain.Book.Language.FRENCH;

@RestController
@RequestMapping(value = "/api/v1", produces = "application/json")
public class BooksController {

    private final BookService bookService;
    private final BooksMapper booksMapper;
    private final AuthorService authorService;
    private final AuthorMapper authorMapper;

    @Autowired
    public BooksController(BookService bookService, BooksMapper booksMapper, AuthorService authorService, AuthorMapper authorMapper) {
        this.bookService = bookService;
        this.booksMapper = booksMapper;
        this.authorService = authorService;
        this.authorMapper = authorMapper;
    }

    @GetMapping("/books")
    public Collection<BookDTO> books(@RequestParam(value = "q", required = false) String query) {
        Collection<Book> books;

        if (query == null) {
            books = bookService.list();
        } else {
            books = bookService.findByTitle(query);
        }

        return books.stream().map(booksMapper::entityToDTO).toList();

    }

    @GetMapping("/books/{id}")
    public BookDTO book(@PathVariable("id") Long id) {

        try {
            var book = this.bookService.get(id);
            if ( book == null ) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return booksMapper.entityToDTO(book);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }

    }

    @PostMapping("/authors/{id}/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookDTO newBook(@PathVariable("id") Long authorId, @RequestBody BookDTO book) throws EntityNotFoundException {

        boolean languageUnknown = true;
        if (book.language() != null) {
            for (Book.Language language : Book.Language.values()) {
                if (language.name().equalsIgnoreCase(book.language())) {
                    languageUnknown = false;
                    break;
                }
            }
            if (languageUnknown) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        }
        Book bookEntity = booksMapper.dtoToEntity(book);
        var isbnLength = String.valueOf(bookEntity.getIsbn()).length();
        Long isbn = bookEntity.getIsbn();
        String titre = bookEntity.getTitle();
        short year = bookEntity.getYear();

        try {
            Author author = this.authorService.get(authorId);
            if ((titre == null) || (titre.trim().isEmpty()) || (year < -9999 || year > 9999) || (isbn == null) ||  (isbnLength > 13 || isbnLength < 10)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }


            bookService.save(authorId, bookEntity);
            return booksMapper.entityToDTO(bookEntity);


    }

    @PutMapping("/books/{id}")
    public BookDTO updateBook(@PathVariable("id") Long authorId, @RequestBody BookDTO book) {
        // attention BookDTO.id() doit être égale à id, sinon la requête utilisateur est mauvaise
        if (book.id() != authorId){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        /*checking if the book exists*/
        try{
            var bookExist = this.bookService.get(book.id());
            var authorExist = this.authorService.get(authorId);
            if (bookExist == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            if (authorExist == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }

            /*book update*/
            Book updatedBook = booksMapper.dtoToEntity(book);
            updatedBook = this.bookService.update(updatedBook);

            updatedBook.addAuthor(authorExist);
            return booksMapper.entityToDTO(updatedBook);

        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "the book given is not found");
        }
    }

    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable("id") Long id) throws EntityNotFoundException, DeleteAuthorException {
        try{
            this.bookService.delete(id);
        }catch(EntityNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Can't delete a book that doesn't exist");
        }
    }


    @PutMapping("books/{id}/authors")
    @ResponseStatus(HttpStatus.OK)
    /* On a changé le type de retour de la fonction en BookDTO,
       et le premier paramètre en bookId car on peut déjà récupérer l'id
       de l'auteur depuis le 2ème paramètre "author" or qu'on ne peut pas
       récupérer l'id du book contenu dans l'endpoint qu'on doit mettre à jour
     */
    public BookDTO addAuthor(@PathVariable("id") Long bookId, @RequestBody AuthorDTO author) throws EntityNotFoundException {
        Book book = bookService.get(bookId);
        Author authorEntity = authorMapper.dtoToEntity(author);
        book.addAuthor(authorEntity);
        BookDTO bookDTO = booksMapper.entityToDTO(book);
        return bookDTO;
    }

}

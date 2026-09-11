package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import com.ibizabroker.bibliotheque.exceptions.ForbiddenException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import com.ibizabroker.bibliotheque.service.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Contrôleur REST pour la gestion des emprunts.
 *
 * Matrice des autorisations (cohérente avec le module Réservation) :
 *
 * | Endpoint | Anonyme | ADHERENT | BIBLIOTHECAIRE |
 * |----------|---------|----------|----------------|
 * | POST /borrow | NON | OUI, pour lui-même (RS-04) | OUI, pour tous |
 * | GET /borrow | NON | NON (403) | OUI, tous |
 * | PUT /borrow (retour) | NON | OUI, si emprunt lui appartient (RS-03) | OUI, tous |
 * | GET /borrow/user/{id} | NON | OUI, si {id} == lui-même (RS-03) | OUI, tous |
 * | GET /borrow/book/{id} | NON | OUI | OUI |
 *
 * RS-04 : L'identité de l'emprunteur provient TOUJOURS du token JWT pour un ADHERENT,
 *         jamais du corps de la requête.
 * RS-03 : Un ADHERENT ne peut consulter/retourner que ses propres emprunts.
 * RG-06 : Un retour incrémente les copies et fait avancer la file des réservations.
 */
@RestController
@RequestMapping("/borrow")
public class BorrowController {

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private com.ibizabroker.bibliotheque.service.ReservationService reservationService;

    /**
     * POST /borrow
     * RS-04 : Pour un ADHERENT, l'identité vient du token (userId du body ignoré).
     * Le BIBLIOTHECAIRE peut emprunter au nom de n'importe quel adhérent.
     */
    @PostMapping
    public String borrowBook(@RequestBody Borrow borrow) {
        // RS-04 : pour un ADHERENT, on ignore userId du body et on utilise l'identité du token
        if (currentUserService.isAdherent()) {
            borrow.setUserId(currentUserService.getCurrentUserId());
        }

        Users user = usersRepository.findById(borrow.getUserId())
                .orElseThrow(() -> new NotFoundException(
                        "User with id " + borrow.getUserId() + " does not exist."));
        Books book = booksRepository.findById(borrow.getBookId())
                .orElseThrow(() -> new NotFoundException(
                        "Book with id " + borrow.getBookId() + " does not exist."));

        if (book.getNoOfCopies() < 1) {
            throw new ConflictException(
                    "Le livre \"" + book.getBookName() + "\" est épuisé : aucun exemplaire disponible.");
        }

        book.borrowBook();
        booksRepository.save(book);

        // RG-06 : si cet adhérent avait réservé ce livre (DISPONIBLE pour lui), la réservation passe à HONOREE
        reservationService.honorerReservationSiExistante(book.getBookId(), user.getUserId());

        Date currentDate = new Date();
        Date overdueDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(overdueDate);
        c.add(Calendar.DATE, 7);
        overdueDate = c.getTime();
        borrow.setIssueDate(currentDate);
        borrow.setDueDate(overdueDate);
        borrowRepository.save(borrow);
        return user.getName() + " has borrowed one copy of \"" + book.getBookName() + "\"!";
    }

    /**
     * GET /borrow
     * RS-05 : La liste globale des emprunts est réservée au BIBLIOTHECAIRE.
     */
    @PreAuthorize("hasRole('Admin')")
    @GetMapping
    public List<Borrow> getAllBorrow() {
        return borrowRepository.findAll();
    }

    /**
     * PUT /borrow
     * RS-03 : Un ADHERENT ne peut retourner que SON propre emprunt.
     * RG-06 : Un retour incrémente les copies et fait avancer la file des réservations.
     */
    @PutMapping
    public Borrow returnBook(@RequestBody Borrow borrow) {
        Borrow borrowBook = borrowRepository.findById(borrow.getBorrowId())
                .orElseThrow(() -> new NotFoundException(
                        "Borrow with id " + borrow.getBorrowId() + " does not exist."));

        // RS-03 : vérification de propriété pour un ADHERENT
        if (currentUserService.isAdherent()
                && !borrowBook.getUserId().equals(currentUserService.getCurrentUserId())) {
            throw new ForbiddenException("RS-03 : cet emprunt ne vous appartient pas.");
        }

        Books book = booksRepository.findById(borrowBook.getBookId())
                .orElseThrow(() -> new NotFoundException(
                        "Book with id " + borrowBook.getBookId() + " does not exist."));

        book.returnBook();
        booksRepository.save(book);

        // RG-06 : un exemplaire redevient disponible -> la plus ancienne réservation EN_ATTENTE passe à DISPONIBLE
        reservationService.promouvoirProchaineReservation(book.getBookId());

        Date currentDate = new Date();
        borrowBook.setReturnDate(currentDate);
        return borrowRepository.save(borrowBook);
    }

    /**
     * GET /borrow/user/{id}
     * RS-03 : Un ADHERENT ne peut consulter que SON propre historique.
     * Le BIBLIOTHECAIRE peut consulter l'historique de tous.
     */
    @GetMapping("user/{id}")
    public List<Borrow> booksBorrowedByUser(@PathVariable Integer id) {
        // RS-03 : vérification de propriété pour un ADHERENT
        if (currentUserService.isAdherent() && !id.equals(currentUserService.getCurrentUserId())) {
            throw new ForbiddenException("RS-03 : vous ne pouvez consulter que votre propre historique.");
        }
        return borrowRepository.findByUserId(id);
    }

    @GetMapping("book/{id}")
    public List<Borrow> bookBorrowHistory(@PathVariable Integer id) {
        return borrowRepository.findByBookId(id);
    }


//    @Autowired
//    private EntityManager entityManager;
//
//    @PostMapping
//    public Borrow borrowBook(@RequestBody Borrow borrow) {
//        borrowRepository.save(borrow);
//        Books book = booksRepository.findById(borrow.getBOOKID()).orElseThrow(() -> new NotFoundException("Book not found."));
//        if(book.getNoOfCopies()-1 < 0) {
//            throw new IllegalStateException("There are no available books.");
//        }
//        book.borrowBook();
//        booksRepository.save(book);
//
//        return borrow;
//    }
//
//    @GetMapping
//    public List<Borrow> getAllBorrow() {
//        return borrowRepository.findAll();
//    }
//
//    @PutMapping
//    public Borrow returnBook(@RequestBody Borrow borrow) {
//        borrowRepository.save(borrow);
//        Books book = booksRepository.findById(borrow.getBOOKID()).orElseThrow(() -> new NotFoundException("Book not found."));
//        book.returnBook();
//        booksRepository.save(book);
//
//        Date currentDate = new Date(new java.util.Date().getTime());
//        borrow.setReturnDate(currentDate);
//        return borrow;
//    }
//
//    @GetMapping("user/{id}")
//    public List<Books> booksBorrowedByUser(@PathVariable Integer id) {
//        Query q = entityManager.createNativeQuery("SELECT * FROM BOOKS AS B, BORROW AS L WHERE B.book_id = L.BOOKID AND L.USERID = " + id);
//        List<Books> borrowedBooks = q.getResultList();
//        return borrowedBooks;
//    }
//
//    @GetMapping("book/{id}")
//    public List<Users> bookBorrowHistory(@PathVariable Integer id) {
//        Query q = entityManager.createNativeQuery("SELECT * FROM USERS AS U, BORROW AS L WHERE U.user_id = L.USERID AND L.BOOKID = " + id);
//        List<Users> usersList = q.getResultList();
//        return usersList;
//    }

//    @PostMapping
//    public Borrow borrowBook(@RequestBody Borrow borrow) {
//        borrow(borrow.getBorrowId(), borrow.getUser().getUserId(), borrow.getBook().getBookId());
//        return borrow;
//    }
//
//    @GetMapping
//    public List<Borrow> getAllBorrow() {
//        return borrowRepository.findAll();
//    }
//
//    @PutMapping
//    public Borrow returnBook(@RequestBody Borrow borrow) {
//        Books book = booksRepository.findById(borrow.getBook().getBookId()).orElseThrow(() -> new NotFoundException("Book not found."));
//        book.returnBook();
//        booksRepository.save(book);
//
//        Date currentDate = new Date(new java.util.Date().getTime());
//        borrow.setReturnDate(currentDate);
//        return borrowRepository.save(borrow);
//    }
//
//    @GetMapping("user/{id}")
//    public List<Books> booksBorrowedByUser(@PathVariable Integer id) {
//        Users user = usersRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found."));
//        return user.getBooks();
//    }
//
//    @GetMapping("book/{id}")
//    public List<Users> bookBorrowHistory(@PathVariable Integer id) {
//        Books book = booksRepository.findById(id).orElseThrow(() -> new NotFoundException("Book not found."));
//        return book.getUsers();
//    }
//
//    public void borrow(Integer borrowId, Integer userId, Integer bookId) {
//        Users user = usersRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));
//        if(user.getBooks().stream().anyMatch(book -> Objects.equals(book.getBookId(), bookId))) {
//            throw new IllegalStateException("User already borrowed the book");
//        }
//
//        Books book = booksRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Book not found."));
//        if(book.getNoOfCopies()-1 < 0) {
//            throw new IllegalStateException("There are no available books.");
//        }
//
//        book.getUsers().add(user);
//        book.setNoOfCopies(book.getNoOfCopies()-1);
//        booksRepository.save(book);
//
//        user.getBooks().add(book);
//        usersRepository.save(user);
//    }

}

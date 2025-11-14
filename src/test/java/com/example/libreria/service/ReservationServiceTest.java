package com.example.libreria.service;

import com.example.libreria.dto.ReservationRequestDTO;
import com.example.libreria.dto.ReservationResponseDTO;
import com.example.libreria.dto.ReturnBookRequestDTO;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookService bookService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReservationService reservationService;

    private User testUser;
    private Book testBook;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@example.com");

        testBook = new Book();
        testBook.setExternalId(258027L);
        testBook.setTitle("The Lord of the Rings");
        testBook.setPrice(new BigDecimal("15.99"));
        testBook.setStockQuantity(10);
        testBook.setAvailableQuantity(5);

        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setBook(testBook);
        testReservation.setRentalDays(7);
        testReservation.setStartDate(LocalDate.now());
        testReservation.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testReservation.setDailyRate(new BigDecimal("15.99"));
        testReservation.setTotalFee(new BigDecimal("111.93"));
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateReservation_Success() {
        // TO DO: Implementar el test de creación de reserva exitosa
        ReservationRequestDTO request = new ReservationRequestDTO();
        request.setUserId(1L);
        request.setBookExternalId(258027L);
        request.setRentalDays(7);
        request.setStartDate(LocalDate.now());

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookRepository.findById(258027L)).thenReturn(Optional.of(testBook));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        ReservationResponseDTO result = reservationService.createReservation(request);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(258027L, result.getBookExternalId());
        assertEquals(testBook.getPrice(), result.getDailyRate());
        assertEquals(new BigDecimal("111.93"), result.getTotalFee());

        verify(bookRepository).save(any(Book.class));
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void testCreateReservation_BookNotAvailable() {
        // TO
        // DO: Implementar el test de creación de reserva cuando el libro no está disponible
        testBook.setAvailableQuantity(0);

        ReservationRequestDTO request = new ReservationRequestDTO();
        request.setUserId(1L);
        request.setBookExternalId(258027L);
        request.setRentalDays(7);
        request.setStartDate(LocalDate.now());

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookRepository.findById(258027L)).thenReturn(Optional.of(testBook));

        assertThrows(RuntimeException.class, () -> {
            reservationService.createReservation(request);
        });

        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void testReturnBook_OnTime() {
        // TO DO: Implementar el test de devolución de libro en tiempo
        LocalDate expectedReturn = LocalDate.now();
        testReservation.setExpectedReturnDate(expectedReturn);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnBookRequestDTO request = new ReturnBookRequestDTO();
        request.setReturnDate(expectedReturn);

        ReservationResponseDTO result = reservationService.returnBook(1L, request);

        assertNotNull(result);
        assertEquals(Reservation.ReservationStatus.RETURNED, result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getLateFee());

        verify(bookService).increaseAvailableQuantity(testBook.getExternalId());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void testReturnBook_Overdue() {
        // TO DO: Implementar el test de devolución de libro con retraso
        LocalDate expectedReturn = LocalDate.now().minusDays(3);
        testReservation.setExpectedReturnDate(expectedReturn);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnBookRequestDTO request = new ReturnBookRequestDTO();
        request.setReturnDate(LocalDate.now());

        ReservationResponseDTO result = reservationService.returnBook(1L, request);

        assertNotNull(result);
        assertEquals(Reservation.ReservationStatus.RETURNED, result.getStatus());
        assertEquals(new BigDecimal("7.20"), result.getLateFee());

        verify(bookService).increaseAvailableQuantity(testBook.getExternalId());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void testGetReservationById_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        ReservationResponseDTO result = reservationService.getReservationById(1L);

        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
    }

//    @Test
//    void testGetAllReservations() {
//        Reservation reservation2 = new Reservation();
//        reservation2.setId(2L);
//
//        when(reservationRepository.findAll()).thenReturn(Arrays.asList(testReservation, reservation2));
//
//        List<ReservationResponseDTO> result = reservationService.getAllReservations();
//
//        assertNotNull(result);
//        assertEquals(2, result.size());
//    }

    @Test
    void testGetReservationsByUserId() {
        when(reservationRepository.findByUserId(1L)).thenReturn(Arrays.asList(testReservation));

        List<ReservationResponseDTO> result = reservationService.getReservationsByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetActiveReservations() {
        when(reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE))
                .thenReturn(Arrays.asList(testReservation));

        List<ReservationResponseDTO> result = reservationService.getActiveReservations();

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}

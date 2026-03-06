package com.booking_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.booking_api.dto.BookingRequest;
import com.booking_api.dto.BookingResponse;
import com.booking_api.exception.BookingConflictException;
import com.booking_api.exception.ResourceNotFoundException;
import com.booking_api.model.Booking;
import com.booking_api.model.Room;
import com.booking_api.model.Status;
import com.booking_api.model.User;
import com.booking_api.repository.BookingRepository;
import com.booking_api.repository.RoomRepository;
import com.booking_api.repository.UserRepository;
import com.booking_api.testutil.TestDataFactory;

import io.jsonwebtoken.lang.Collections;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest
{
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    // --- getAllBookings ---

    @Test
    void getAllBookings_shouldReturnList()
    {
        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        Booking booking = TestDataFactory.createConfirmedBooking(1L, user, room, startTime, startTime);

        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        List<BookingResponse> result = bookingService.getAllBookings();

        assertEquals(1, result.size());
        assertEquals("test@test.com", result.get(0).userEmail());
        assertEquals("Room A", result.get(0).roomName());
    }

    @Test
    void getAllBookings_shouldReturnEmptyList()
    {
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        List<BookingResponse> result = bookingService.getAllBookings();

        assertTrue(result.isEmpty());
    }

    // --- getBookingById ---
    @Test
    void getBookingById_shouldReturnBooking()
    {
         User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking booking = TestDataFactory.createConfirmedBooking(
            1L, user, room, start, start.plusHours(1));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BookingResponse result = bookingService.getBookingById(1L);

        assertEquals(1L, result.id());
        assertEquals("test@test.com", result.userEmail());
        assertEquals("CONFIRMED", result.status());
    }

    @Test
    void getBookingById_shouldThrowWhenNotFound()
    {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> bookingService.getBookingById(99L));
    }

    // --- createBooking ---

    @Test
    void createBooking_shouldSaveWhenNoConflict()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        BookingRequest request = new BookingRequest(1L, 1L, start, end);

        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        Booking saved = TestDataFactory.createConfirmedBooking(1L, user, room, start, end);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any()))
            .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        BookingResponse response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("test@test.com", response.userEmail());
        assertEquals("Room A", response.roomName());
        assertEquals("CONFIRMED", response.status());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_shouldRejectPastStartTime()
    {
        BookingRequest request = new BookingRequest(
            1L, 1L,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        );

        assertThrows(IllegalArgumentException.class,
            () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldRejectInvalidTimeRange()
    {
        LocalDateTime future = LocalDateTime.now().plusDays(1);

        BookingRequest request = new BookingRequest(
            1L, 1L,
            future.plusHours(2),
            future
        );

        assertThrows(IllegalArgumentException.class,
            () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldThrowWhenUserNotFound()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingRequest request = new BookingRequest(99L, 1L, start, start.plusHours(1));

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldThrowWhenRoomNotFound()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingRequest request = new BookingRequest(1L, 99L, start, start.plusHours(1));

        User user = TestDataFactory.createDefaultUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldRejectConflictingBooking()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(1);
        BookingRequest request = new BookingRequest(1L, 1L, start, end);

        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        Booking conflict = TestDataFactory.createConfirmedBooking(99L, user, room, start, end);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookings(eq(1L), any(), any()))
            .thenReturn(List.of(conflict));

        assertThrows(BookingConflictException.class,
            () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
    }

    // --- updateBooking ---

    @Test
    void updateBooking_shouldUpdateSuccessfully()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(1);
        BookingRequest request = new BookingRequest(1L, 1L, start, end);

        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        Booking existing = TestDataFactory.createConfirmedBooking(
            1L, user, room, LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(1));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookingsExcluding(eq(1L), eq(1L), any(), any()))
            .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(existing);

        BookingResponse response = bookingService.updateBooking(1L, request);

        assertNotNull(response);
        verify(bookingRepository).save(existing);
    }

    @Test
    void updateBooking_shouldThrowWhenNotFound()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        BookingRequest request = new BookingRequest(1L, 1L, start, start.plusHours(1));

        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> bookingService.updateBooking(99L, request));
    }

    @Test
    void updateBooking_shouldThrowWhenConflicting()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(1);
        BookingRequest request = new BookingRequest(1L, 1L, start, end);

        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        Booking existing = TestDataFactory.createConfirmedBooking(
            1L, user, room, LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(1).plusHours(1));
        Booking conflict = TestDataFactory.createConfirmedBooking(99L, user, room, start, end);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.findConflictingBookingsExcluding(eq(1L), eq(1L), any(), any()))
            .thenReturn(List.of(conflict));

        assertThrows(BookingConflictException.class,
            () -> bookingService.updateBooking(1L, request));

        verify(bookingRepository, never()).save(any());
    }

    // --- cancelBooking ---

    @Test
    void cancelBooking_shouldSetStatusToCancelled()
    {
        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking booking = TestDataFactory.createConfirmedBooking(
            1L, user, room, start, start.plusHours(1));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        bookingService.cancelBooking(1L, "test@test.com");

        assertEquals(Status.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_shouldRejectWhenNotOwner()
    {
        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking booking = TestDataFactory.createConfirmedBooking(
            1L, user, room, start, start.plusHours(1));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class,
            () -> bookingService.cancelBooking(1L, "other@test.com"));

        verify(bookingRepository, never()).save(any());
    }

    // --- adminCancelBooking ---

    @Test
    void adminCancelBooking_shouldSetStatusToCancelled()
    {
        User user = TestDataFactory.createDefaultUser();
        Room room = TestDataFactory.createDefaultRoom();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Booking booking = TestDataFactory.createConfirmedBooking(
            1L, user, room, start, start.plusHours(1));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        bookingService.adminCancelBooking(1L);

        assertEquals(Status.CANCELLED, booking.getStatus());
        verify(bookingRepository).save(booking);
    }
}

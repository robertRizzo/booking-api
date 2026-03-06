package com.booking_api.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.booking_api.model.Booking;
import com.booking_api.model.Role;
import com.booking_api.model.Room;
import com.booking_api.model.Status;
import com.booking_api.model.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryTest
{
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Room room;
    private User user;

    @BeforeEach
    void setUp()
    {
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("test@test.com");
        user.setPassword("password");
        user.setRole(Role.USER);
        user = userRepository.save(user);

        room = new Room();
        room.setName("Test Room");
        room.setCapacity(10);
        room = roomRepository.save(room);
    }

    // --- findConflictingBookings ---

    @Test
    void findConflictingBookings_shouldDetectOverlap()
    {
        LocalDateTime tenAM = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime elevenAM = LocalDateTime.of(2026, 6, 1, 11, 0);

        Booking existing = new Booking();
        existing.setUser(user);
        existing.setRoom(room);
        existing.setStartTime(tenAM);
        existing.setEndTime(elevenAM);
        existing.setStatus(Status.CONFIRMED);
        bookingRepository.save(existing);

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(), tenAM.plusMinutes(30), elevenAM.plusMinutes(30));

        assertFalse(conflicts.isEmpty());
        assertEquals(1, conflicts.size());
    }

    @Test
    void findConflictingBookings_shouldNotDetectNonOverlap()
    {
        LocalDateTime tenAM = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime elevenAM = LocalDateTime.of(2026, 6, 1, 11, 0);

        Booking existing = new Booking();
        existing.setUser(user);
        existing.setRoom(room);
        existing.setStartTime(tenAM);
        existing.setEndTime(elevenAM);
        existing.setStatus(Status.CONFIRMED);
        bookingRepository.save(existing);

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(), elevenAM, elevenAM.plusHours(1));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void findConflictingBookings_shouldIgnoreCancelledBookings()
    {
        LocalDateTime tenAM = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime elevenAM = LocalDateTime.of(2026, 6, 1, 11, 0);

        Booking cancelled = new Booking();
        cancelled.setUser(user);
        cancelled.setRoom(room);
        cancelled.setStartTime(tenAM);
        cancelled.setEndTime(elevenAM);
        cancelled.setStatus(Status.CANCELLED);
        bookingRepository.save(cancelled);

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(), tenAM, elevenAM);

        assertTrue(conflicts.isEmpty());
    }

    // --- findConflictingBookingsExcluding ---

    @Test
    void findConflictingBookingsExcluding_shouldExcludeOwnBooking()
    {
        LocalDateTime tenAM = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime elevenAM = LocalDateTime.of(2026, 6, 1, 11, 0);

        Booking own = new Booking();
        own.setUser(user);
        own.setRoom(room);
        own.setStartTime(tenAM);
        own.setEndTime(elevenAM);
        own.setStatus(Status.CONFIRMED);
        own = bookingRepository.save(own);

        List<Booking> conflicts = bookingRepository.findConflictingBookingsExcluding(
            room.getId(), own.getId(), tenAM, elevenAM);

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void findConflictingBookingsExcluding_shouldDetectOtherConflicts()
    {
        LocalDateTime tenAM = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime elevenAM = LocalDateTime.of(2026, 6, 1, 11, 0);

        Booking own = new Booking();
        own.setUser(user);
        own.setRoom(room);
        own.setStartTime(tenAM.plusHours(2));
        own.setEndTime(elevenAM.plusHours(2));
        own.setStatus(Status.CONFIRMED);
        own = bookingRepository.save(own);

        Booking other = new Booking();
        other.setUser(user);
        other.setRoom(room);
        other.setStartTime(tenAM);
        other.setEndTime(elevenAM);
        other.setStatus(Status.CONFIRMED);
        bookingRepository.save(other);

        List<Booking> conflicts = bookingRepository.findConflictingBookingsExcluding(
            room.getId(), own.getId(), tenAM.plusMinutes(30), elevenAM.plusMinutes(30));

        assertEquals(1, conflicts.size());
    }

    // --- findByUserEmail ---

    @Test
    void findByUserEmail_shouldReturnUserBookings()
    {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setStartTime(LocalDateTime.of(2026, 6, 1, 10, 0));
        booking.setEndTime(LocalDateTime.of(2026, 6, 1, 11, 0));
        booking.setStatus(Status.CONFIRMED);
        bookingRepository.save(booking);

        List<Booking> results = bookingRepository.findByUserEmail("test@test.com");

        assertEquals(1, results.size());
        assertEquals("test@test.com", results.get(0).getUser().getEmail());
    }
}

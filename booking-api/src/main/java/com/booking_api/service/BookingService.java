package com.booking_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.booking_api.model.Booking;
import com.booking_api.model.Status;
import com.booking_api.repository.BookingRepository;
import com.booking_api.repository.RoomRepository;
import com.booking_api.repository.UserRepository;

@Service
public class BookingService
{
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    
    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          UserRepository userRepository
    )
    {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public List<Booking> getAllBookings()
    {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id)
    {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
    }

    public Booking createBooking(Booking booking)
    {
        if(booking.getStartTime().isAfter(booking.getEndTime()) || booking.getStartTime().isEqual(booking.getEndTime()))
        {
            throw new IllegalArgumentException("Invalid time range");
        }

        if(booking.getStartTime().isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Booking cannot start in the past");
        }

        userRepository.findById(booking.getUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        roomRepository.findById(booking.getRoom().getId())
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            booking.getRoom().getId(),
            booking.getStartTime(),
            booking.getEndTime()
        );

        if(!conflicts.isEmpty())
        {
            throw new IllegalStateException("Room is already booked during this time");
        }

        return bookingRepository.save(booking);
    }

    public Booking updateBooking(Long id, Booking bookingDetails)
    {
        Booking existing = bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));
        
        if(bookingDetails.getStartTime().isAfter(bookingDetails.getEndTime())
            || bookingDetails.getStartTime().isEqual(bookingDetails.getEndTime()))
        {
            throw new IllegalArgumentException("Invalid time range");
        }

        if(bookingDetails.getStartTime().isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Booking cannot start in the past");
        }

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            bookingDetails.getRoom().getId(),
            bookingDetails.getStartTime(),
            bookingDetails.getEndTime()
        );

        conflicts.removeIf(b -> b.getId().equals(id));

        if(!conflicts.isEmpty())
        {
            throw new IllegalArgumentException("Room is already booked during this time");
        }

        existing.setRoom(bookingDetails.getRoom());
        existing.setStartTime(bookingDetails.getStartTime());
        existing.setEndTime(bookingDetails.getEndTime());

        return bookingRepository.save(existing);
    }

    public void cancelBooking(Long id)
    {
        Booking existing = bookingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + id));

        existing.setStatus(Status.CANCELLED);
        bookingRepository.save(existing);
    }

    public Booking updateBookingStatus(Long id, Status status)
    {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        validateStatusTransition(booking.getStatus(), status);

        booking.setStatus(status);

        return bookingRepository.save(booking);
    }

    private void validateStatusTransition(Status current, Status newStatus)
    {
        if(current == Status.CANCELLED || current == Status.COMPLETED)
        {
            throw new RuntimeException("Cannot modify completed or cancelled booking");
        }

        if(current == Status.PENDING && newStatus == Status.COMPLETED)
        {
            throw new RuntimeException("Pending booking cannot be completed directly");
        }
    }
}

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

    public Booking updateBookingStatus(Long bookingId, Status status)
    {
        Booking booking = bookingRepository.findById(bookingId)
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

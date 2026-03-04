package com.booking_api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

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

    public List<BookingResponse> getAllBookings()
    {
        return bookingRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public List<BookingResponse> getBookingsByUserEmail(String email)
    {
        return bookingRepository.findByUserEmail(email).stream()
            .map(this::toResponse)
            .toList();
    }

    public BookingResponse getBookingById(Long id)
    {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
            return toResponse(booking);
    }

    public BookingResponse createBooking(BookingRequest request)
    {
        validateTimeRange(request.startTime(), request.endTime());

        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.userId()));

        Room room = roomRepository.findById(request.roomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id:" + request.roomId()));

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(),
            request.startTime(),
            request.endTime()
        );

        if(!conflicts.isEmpty())
        {
            throw new BookingConflictException("Room is already booked during this time");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(Status.CONFIRMED);
        
        Booking saved = bookingRepository.save(booking);
        return toResponse(saved);
    }

    public BookingResponse updateBooking(Long id, BookingRequest request)
    {
        Booking existing = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        
        validateTimeRange(request.startTime(), request.endTime());

        Room room = roomRepository.findById(request.roomId())
            .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.roomId()));

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
            room.getId(),
            request.startTime(),
            request.endTime()
        );

        conflicts.removeIf(b -> b.getId().equals(id));

        if(!conflicts.isEmpty())
        {
            throw new ResourceNotFoundException("Room is already booked during this time");
        }

        existing.setRoom(room);
        existing.setStartTime(request.startTime());
        existing.setEndTime(request.endTime());

        Booking saved = bookingRepository.save(existing);

        return toResponse(saved);
    }

    public void cancelBooking(Long id, String currentUserEmail)
    {
        Booking existing = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if(!existing.getUser().getEmail().equals(currentUserEmail))
        {
            throw new IllegalStateException("You can only cancel your own bookings");
        }

        existing.setStatus(Status.CANCELLED);
        bookingRepository.save(existing);
    }

    public void adminCancelBooking(Long id)
    {
        Booking existing = bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        existing.setStatus(Status.CANCELLED);
        bookingRepository.save(existing);
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime)
    {
        if(startTime.isAfter(endTime) || startTime.isEqual(endTime))
        {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if(startTime.isBefore(LocalDateTime.now()))
        {
            throw new IllegalArgumentException("Booking cannot start in the past");
        }
    }

    private BookingResponse toResponse(Booking booking)
    {
        return new BookingResponse(
            booking.getId(),
            booking.getUser().getId(),
            booking.getUser().getEmail(),
            booking.getRoom().getId(),
            booking.getRoom().getName(),
            booking.getStartTime(),
            booking.getEndTime(),
            booking.getStatus().name()
        );
    }
}

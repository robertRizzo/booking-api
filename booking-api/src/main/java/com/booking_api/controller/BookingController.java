package com.booking_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.model.Booking;
import com.booking_api.model.Status;
import com.booking_api.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
public class BookingController
{
    private final BookingService bookingService;

    public BookingController(BookingService bookingService)
    {
        this.bookingService= bookingService;
    }
    // @GetMapping
    
    @PostMapping
    public Booking createBooking(@RequestBody Booking booking)
    {
        return bookingService.createBooking(booking);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Booking> updateStatus(
        @PathVariable Long id,
        @RequestParam Status status
    ) 
    {
        return ResponseEntity.ok(
            bookingService.updateBookingStatus(id, status)
        );
    }

    // @DeleteMapping     
}

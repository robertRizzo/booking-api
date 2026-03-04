package com.booking_api.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booking_api.model.Booking;
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

    // @PutMapping

    // @DeleteMapping     
}

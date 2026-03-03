package com.booking_api.service;

import org.springframework.stereotype.Service;

import com.booking_api.repository.BookingRepository;

@Service
public class BookingService
{
    private final BookingRepository bookingRepository;
    
    public BookingService(BookingRepository bookingRepository)
    {
        this.bookingRepository = bookingRepository;
    }
}

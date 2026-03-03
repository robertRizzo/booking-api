package com.booking_api.service;

import org.springframework.stereotype.Service;

import com.booking_api.repository.RoomRepository;

@Service
public class RoomService
{
    private final RoomRepository roomRepository;
    
    public RoomService(RoomRepository roomRepository)
    {
        this.roomRepository = roomRepository;
    }
}

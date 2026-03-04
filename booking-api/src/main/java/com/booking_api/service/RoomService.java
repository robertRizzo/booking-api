package com.booking_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.booking_api.model.Room;
import com.booking_api.repository.RoomRepository;

@Service
public class RoomService
{
    private final RoomRepository roomRepository;
    
    public RoomService(RoomRepository roomRepository)
    {
        this.roomRepository = roomRepository;
    }

    public Room getRoomById(Long id)
    {
        return roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));
    }

    public List<Room> getAllRooms()
    {
        return roomRepository.findAll();
    }

    public Room createRoom(Room room)
    {
        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, Room roomDetails)
    {
        Room existing = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));

        existing.setName(roomDetails.getName());
        existing.setCapacity(roomDetails.getCapacity());

        return roomRepository.save(existing);
    }

    public void deleteRoom(Long id)
    {
        if(!roomRepository.existsById(id))
        {
            throw new IllegalArgumentException("Room not found with id:" + id);
        }
        
        roomRepository.deleteById(id);
    }
}

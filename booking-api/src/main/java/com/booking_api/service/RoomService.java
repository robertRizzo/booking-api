package com.booking_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.RoomResponse;
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

    public List<RoomResponse> getAllRooms()
    {
        return roomRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public RoomResponse getRoomById(Long id)
    {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));
        return toResponse(room);
    }

    public RoomResponse createRoom(RoomRequest request)
    {
        Room room = new Room();
        room.setName(request.name());
        room.setCapacity(request.capacity());

        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    public RoomResponse updateRoom(Long id, RoomRequest request)
    {
        Room existing = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));

        existing.setName(request.name());
        existing.setCapacity(request.capacity());

        Room saved = roomRepository.save(existing);
        return toResponse(saved);
    }

    public void deleteRoom(Long id)
    {
        if(!roomRepository.existsById(id))
        {
            throw new IllegalArgumentException("Room not found with id:" + id);
        }
        
        roomRepository.deleteById(id);
    }

    private RoomResponse toResponse(Room room)
    {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity());
    }
}

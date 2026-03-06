package com.booking_api.service;

import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.RoomResponse;
import com.booking_api.exception.ResourceNotFoundException;
import com.booking_api.model.Room;
import com.booking_api.repository.RoomRepository;
import com.booking_api.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest
{
    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    // --- getAllRooms ---

    @Test
    void getAllRooms_shouldReturnList()
    {
        Room room = TestDataFactory.createDefaultRoom();
        when(roomRepository.findAll()).thenReturn(List.of(room));

        List<RoomResponse> result = roomService.getAllRooms();

        assertEquals(1, result.size());
        assertEquals("Room A", result.get(0).name());
    }

    // --- getRoomById ---

    @Test
    void getRoomById_shouldReturnRoom()
    {
        Room room = TestDataFactory.createDefaultRoom();
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        RoomResponse result = roomService.getRoomById(1L);

        assertEquals(1L, result.id());
        assertEquals("Room A", result.name());
        assertEquals(10, result.capacity());
    }

    @Test
    void getRoomById_shouldThrowWhenNotFound()
    {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> roomService.getRoomById(99L));
    }

    // --- createRoom ---

    @Test
    void createRoom_shouldSaveAndReturnResponse()
    {
        RoomRequest request = TestDataFactory.createRoomRequest("New Room", 15);
        Room saved = TestDataFactory.createRoom(1L, "New Room", 15);
        when(roomRepository.save(any(Room.class))).thenReturn(saved);

        RoomResponse result = roomService.createRoom(request);

        assertEquals("New Room", result.name());
        assertEquals(15, result.capacity());
        verify(roomRepository).save(any(Room.class));
    }

    // --- updateRoom ---

    @Test
    void updateRoom_shouldUpdateAndReturnResponse()
    {
        Room existing = TestDataFactory.createRoom(1L, "Old Room", 10);
        Room updated = TestDataFactory.createRoom(1L, "Updated Room", 20);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roomRepository.save(any(Room.class))).thenReturn(updated);

        RoomRequest request = TestDataFactory.createRoomRequest("Updated Room", 20);
        RoomResponse result = roomService.updateRoom(1L, request);

        assertEquals("Updated Room", result.name());
        assertEquals(20, result.capacity());
        verify(roomRepository).save(existing);
    }

    @Test
    void updateRoom_shouldThrowWhenNotFound()
    {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        RoomRequest request = TestDataFactory.createRoomRequest("Any", 5);

        assertThrows(ResourceNotFoundException.class,
            () -> roomService.updateRoom(99L, request));

        verify(roomRepository, never()).save(any());
    }

    // --- deleteRoom ---

    @Test
    void deleteRoom_shouldDeleteWhenExists()
    {
        when(roomRepository.existsById(1L)).thenReturn(true);

        roomService.deleteRoom(1L);

        verify(roomRepository).deleteById(1L);
    }

    @Test
    void deleteRoom_shouldThrowWhenNotFound()
    {
        when(roomRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
            () -> roomService.deleteRoom(99L));

        verify(roomRepository, never()).deleteById(any());
    }
}
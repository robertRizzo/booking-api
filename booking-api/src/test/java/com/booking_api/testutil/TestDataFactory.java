package com.booking_api.testutil;

import java.time.LocalDateTime;

import com.booking_api.dto.BookingRequest;
import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.UserRequest;
import com.booking_api.model.Booking;
import com.booking_api.model.Role;
import com.booking_api.model.Room;
import com.booking_api.model.Status;
import com.booking_api.model.User;

public final class TestDataFactory
{
    private TestDataFactory()
    {}

    public static User createUser(Long id, String email, Role role)
    {
        return new User(id, email, "password", role);
    }

    public static User createDefaultUser()
    {
        return createUser(1L, "test@test.com", Role.USER);
    }

    public static Room createRoom(Long id, String name, int capacity)
    {
        return new Room(id, name, capacity);
    }

    public static Room createDefaultRoom()
    {
        return createRoom(1L, "Room A", 10);
    }

    public static Booking createBooking(Long id, User user, Room room, LocalDateTime startTime, LocalDateTime endTime, Status status)
    {
        return new Booking(id, user, room, startTime, endTime, status);
    }

    public static Booking createConfirmedBooking(Long id, User user, Room room, LocalDateTime startTime, LocalDateTime endTime)
    {
        return createBooking(id, user, room, startTime, endTime, Status.CONFIRMED);
    }

    public static BookingRequest createBookingRequest(Long userId, Long roomId, int daysFromNow)
    {
        LocalDateTime startTime = LocalDateTime.now().plusDays(daysFromNow);
        LocalDateTime endTime = startTime.plusHours(1);
        return new BookingRequest(userId, roomId, startTime, endTime);
    }

    public static RoomRequest createRoomRequest(String name, int capacity)
    {
        return new RoomRequest(name, capacity);
    }

    public static UserRequest createUserRequest(String email, String password, String role)
    {
        return new UserRequest(email, password, role);
    }
}
package com.booking_api.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import com.booking_api.dto.BookingRequest;
import com.booking_api.dto.BookingResponse;
import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.RoomResponse;
import com.booking_api.testutil.IntegrationTestBase;
import com.booking_api.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class BookingControllerIntegrationTest extends IntegrationTestBase
{
    private HttpHeaders headers;
    private Long roomId;

    @BeforeEach
    void setUp()
    {
        headers = adminHeaders();

        RoomRequest roomRequest = TestDataFactory.createRoomRequest(
            "Booking Test Room " + UUID.randomUUID().toString().substring(0, 8), 10);
        HttpEntity<RoomRequest> roomEntity = new HttpEntity<>(roomRequest, headers);
        ResponseEntity<RoomResponse> roomResponse = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, roomEntity, RoomResponse.class);

        assertNotNull(roomResponse.getBody());
        roomId = roomResponse.getBody().id();
    }

    private BookingResponse createTestBooking(int daysFromNow)
    {
        LocalDateTime start = LocalDateTime.now().plusDays(daysFromNow);
        BookingRequest request = new BookingRequest(1L, roomId, start, start.plusHours(1));
        HttpEntity<BookingRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<BookingResponse> response = restTemplate.exchange(
            "/api/bookings", HttpMethod.POST, entity, BookingResponse.class);
        assertNotNull(response.getBody());
        return response.getBody();
    }

    // --- security ---

    @Test
    void createBooking_withoutAuth_shouldReturn401()
    {
        BookingRequest request = new BookingRequest(
            1L, roomId,
            LocalDateTime.now().plusDays(5),
            LocalDateTime.now().plusDays(5).plusHours(1)
        );
        HttpEntity<BookingRequest> entity = new HttpEntity<>(request);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/bookings", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // --- POST /api/bookings ---

    @Test
    void createBooking_asAdmin_shouldReturn201()
    {
        BookingRequest request = new BookingRequest(
            1L, roomId,
            LocalDateTime.now().plusDays(10),
            LocalDateTime.now().plusDays(10).plusHours(1)
        );
        HttpEntity<BookingRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<BookingResponse> response = restTemplate.exchange(
            "/api/bookings", HttpMethod.POST, entity, BookingResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CONFIRMED", response.getBody().status());
        assertEquals(roomId, response.getBody().roomId());
    }

    @Test
    void createBooking_conflicting_shouldReturn409()
    {
        LocalDateTime start = LocalDateTime.now().plusDays(20);
        LocalDateTime end = start.plusHours(1);

        BookingRequest first = new BookingRequest(1L, roomId, start, end);
        HttpEntity<BookingRequest> firstEntity = new HttpEntity<>(first, headers);
        restTemplate.exchange("/api/bookings", HttpMethod.POST, firstEntity, BookingResponse.class);

        BookingRequest overlapping = new BookingRequest(
            1L, roomId,
            start.plusMinutes(30),
            end.plusMinutes(30)
        );
        HttpEntity<BookingRequest> secondEntity = new HttpEntity<>(overlapping, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/bookings", HttpMethod.POST, secondEntity, String.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    // --- GET /api/bookings ---

    @Test
    void getAllBookings_asAdmin_shouldReturn200()
    {
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/bookings", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- GET /api/bookings/my ---

    @Test
    void getMyBookings_asAdmin_shouldReturn200()
    {
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/bookings/my", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- GET /api/bookings/{id} ---

    @Test
    void getBookingById_asAdmin_shouldReturn200()
    {
        BookingResponse created = createTestBooking(30);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<BookingResponse> response = restTemplate.exchange(
            "/api/bookings/" + created.id(), HttpMethod.GET, entity, BookingResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(created.id(), response.getBody().id());
    }

    // --- PUT /api/bookings/{id} ---

    @Test
    void updateBooking_asAdmin_shouldReturn200()
    {
        BookingResponse created = createTestBooking(40);

        LocalDateTime newStart = LocalDateTime.now().plusDays(41);
        BookingRequest updateRequest = new BookingRequest(
            1L, roomId, newStart, newStart.plusHours(2));
        HttpEntity<BookingRequest> entity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<BookingResponse> response = restTemplate.exchange(
            "/api/bookings/" + created.id(), HttpMethod.PUT, entity, BookingResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- DELETE /api/bookings/{id} ---

    @Test
    void cancelBooking_asAdmin_shouldReturn204()
    {
        BookingResponse created = createTestBooking(50);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/bookings/" + created.id(), HttpMethod.DELETE, entity, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}

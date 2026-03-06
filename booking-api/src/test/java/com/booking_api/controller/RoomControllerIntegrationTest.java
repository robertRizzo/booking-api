package com.booking_api.controller;

import java.util.UUID;

import com.booking_api.dto.RoomRequest;
import com.booking_api.dto.RoomResponse;
import com.booking_api.testutil.IntegrationTestBase;
import com.booking_api.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class RoomControllerIntegrationTest extends IntegrationTestBase
{
    // --- security ---

    @Test
    void getRooms_withoutAuth_shouldReturn401()
    {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/rooms", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void createRoom_withoutAuth_shouldReturn401()
    {
        RoomRequest request = TestDataFactory.createRoomRequest("Test Room", 10);
        HttpEntity<RoomRequest> entity = new HttpEntity<>(request);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // --- GET /api/rooms ---

    @Test
    void getRooms_asAdmin_shouldReturn200()
    {
        HttpEntity<Void> entity = new HttpEntity<>(adminHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/rooms", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- GET /api/rooms/{id} ---

    @Test
    void getRoomById_asAdmin_shouldReturn200()
    {
        HttpHeaders headers = adminHeaders();
        String roomName = "Lookup Room " + UUID.randomUUID().toString().substring(0, 8);

        RoomRequest createRequest = TestDataFactory.createRoomRequest(roomName, 8);
        HttpEntity<RoomRequest> createEntity = new HttpEntity<>(createRequest, headers);
        ResponseEntity<RoomResponse> createResponse = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, createEntity, RoomResponse.class);
        Long roomId = createResponse.getBody().id();

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<RoomResponse> response = restTemplate.exchange(
            "/api/rooms/" + roomId, HttpMethod.GET, entity, RoomResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(roomName, response.getBody().name());
    }

    // --- POST /api/rooms ---

    @Test
    void createRoom_asAdmin_shouldReturn201()
    {
        String roomName = "Integration Room " + UUID.randomUUID().toString().substring(0, 8);
        RoomRequest request = TestDataFactory.createRoomRequest(roomName, 5);
        HttpEntity<RoomRequest> entity = new HttpEntity<>(request, adminHeaders());

        ResponseEntity<RoomResponse> response = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, entity, RoomResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(roomName, response.getBody().name());
        assertEquals(5, response.getBody().capacity());
    }

    @Test
    void createRoom_withInvalidData_shouldReturn400()
    {
        RoomRequest request = TestDataFactory.createRoomRequest("", -1);
        HttpEntity<RoomRequest> entity = new HttpEntity<>(request, adminHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // --- PUT /api/rooms/{id} ---

    @Test
    void updateRoom_asAdmin_shouldReturn200()
    {
        HttpHeaders headers = adminHeaders();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        RoomRequest createRequest = TestDataFactory.createRoomRequest("Before Update " + suffix, 4);
        HttpEntity<RoomRequest> createEntity = new HttpEntity<>(createRequest, headers);
        ResponseEntity<RoomResponse> createResponse = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, createEntity, RoomResponse.class);
        Long roomId = createResponse.getBody().id();

        String updatedName = "After Update " + suffix;
        RoomRequest updateRequest = TestDataFactory.createRoomRequest(updatedName, 12);
        HttpEntity<RoomRequest> updateEntity = new HttpEntity<>(updateRequest, headers);
        ResponseEntity<RoomResponse> response = restTemplate.exchange(
            "/api/rooms/" + roomId, HttpMethod.PUT, updateEntity, RoomResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedName, response.getBody().name());
        assertEquals(12, response.getBody().capacity());
    }

    // --- DELETE /api/rooms/{id} ---

    @Test
    void deleteRoom_asAdmin_shouldReturn204()
    {
        HttpHeaders headers = adminHeaders();

        RoomRequest createRequest = TestDataFactory.createRoomRequest(
            "To Delete " + UUID.randomUUID().toString().substring(0, 8), 3);
        HttpEntity<RoomRequest> createEntity = new HttpEntity<>(createRequest, headers);
        ResponseEntity<RoomResponse> createResponse = restTemplate.exchange(
            "/api/rooms", HttpMethod.POST, createEntity, RoomResponse.class);
        Long roomId = createResponse.getBody().id();

        HttpEntity<Void> deleteEntity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/rooms/" + roomId, HttpMethod.DELETE, deleteEntity, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}

package com.booking_api.controller;

import java.util.UUID;

import com.booking_api.dto.UserRequest;
import com.booking_api.dto.UserResponse;
import com.booking_api.testutil.IntegrationTestBase;
import com.booking_api.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerIntegrationTest extends IntegrationTestBase
{
    private String uniqueEmail()
    {
        return "test-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private UserResponse createTestUser(HttpHeaders headers)
    {
        UserRequest request = TestDataFactory.createUserRequest(uniqueEmail(), "password123", "USER");
        HttpEntity<UserRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<UserResponse> response = restTemplate.exchange(
            "/api/users", HttpMethod.POST, entity, UserResponse.class);
        assertNotNull(response.getBody());
        return response.getBody();
    }

    // --- security ---

    @Test
    void getUsers_withoutAuth_shouldReturn401()
    {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/users", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void createUser_withoutAuth_shouldReturn401()
    {
        UserRequest request = TestDataFactory.createUserRequest(uniqueEmail(), "password123", "USER");
        HttpEntity<UserRequest> entity = new HttpEntity<>(request);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/users", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // --- GET /api/users ---

    @Test
    void getUsers_asAdmin_shouldReturn200()
    {
        HttpEntity<Void> entity = new HttpEntity<>(adminHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/users", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- GET /api/users/{id} ---

    @Test
    void getUserById_asAdmin_shouldReturn200()
    {
        HttpHeaders headers = adminHeaders();
        UserResponse created = createTestUser(headers);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<UserResponse> response = restTemplate.exchange(
            "/api/users/" + created.id(), HttpMethod.GET, entity, UserResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(created.email(), response.getBody().email());
    }

    // --- POST /api/users ---

    @Test
    void createUser_asAdmin_shouldReturn201()
    {
        String email = uniqueEmail();
        UserRequest request = TestDataFactory.createUserRequest(email, "password123", "USER");
        HttpEntity<UserRequest> entity = new HttpEntity<>(request, adminHeaders());

        ResponseEntity<UserResponse> response = restTemplate.exchange(
            "/api/users", HttpMethod.POST, entity, UserResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(email, response.getBody().email());
        assertEquals("USER", response.getBody().role());
    }

    @Test
    void createUser_withInvalidEmail_shouldReturn400()
    {
        UserRequest request = TestDataFactory.createUserRequest("not-an-email", "password123", "USER");
        HttpEntity<UserRequest> entity = new HttpEntity<>(request, adminHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/users", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // --- PUT /api/users/{id} ---

    @Test
    void updateUser_asAdmin_shouldReturn200()
    {
        HttpHeaders headers = adminHeaders();
        UserResponse created = createTestUser(headers);

        String updatedEmail = uniqueEmail();
        UserRequest updateRequest = TestDataFactory.createUserRequest(updatedEmail, "newpass123", "ADMIN");
        HttpEntity<UserRequest> entity = new HttpEntity<>(updateRequest, headers);

        ResponseEntity<UserResponse> response = restTemplate.exchange(
            "/api/users/" + created.id(), HttpMethod.PUT, entity, UserResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedEmail, response.getBody().email());
        assertEquals("ADMIN", response.getBody().role());
    }

    // --- DELETE /api/users/{id} ---

    @Test
    void deleteUser_asAdmin_shouldReturn204()
    {
        HttpHeaders headers = adminHeaders();
        UserResponse created = createTestUser(headers);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/users/" + created.id(), HttpMethod.DELETE, entity, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}

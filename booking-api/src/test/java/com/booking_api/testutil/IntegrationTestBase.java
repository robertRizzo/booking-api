package com.booking_api.testutil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase
{
    @Autowired
    protected TestRestTemplate restTemplate;

    protected String loginAndGetToken(String email, String password)
    {
        record LoginRequest(String email, String password)
        {}
        record LoginResponse(String token)
        {}

        LoginRequest request = new LoginRequest(email, password);
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            "/api/auth/login", request, LoginResponse.class);

        assertNotNull(response.getBody(), "Login response body was null");
        return response.getBody().token();
    }

    protected String loginAsAdmin()
    {
        return loginAndGetToken("admin@test.com", "admin123");
    }

    protected HttpHeaders authHeaders(String token)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    protected HttpHeaders adminHeaders()
    {
        return authHeaders(loginAsAdmin());
    }
}

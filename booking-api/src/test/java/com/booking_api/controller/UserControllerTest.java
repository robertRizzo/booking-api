package com.booking_api.controller;

import java.util.List;

import com.booking_api.config.SecurityConfig;
import com.booking_api.dto.UserResponse;
import com.booking_api.exception.ResourceNotFoundException;
import com.booking_api.service.CustomUserDetailsService;
import com.booking_api.service.JwtService;
import com.booking_api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // --- GET /api/users ---

    @Test
    void getAllUsers_withoutAuth_shouldReturn401() throws Exception
    {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_asAdmin_shouldReturn200() throws Exception
    {
        UserResponse user = new UserResponse(1L, "admin@test.com", "ADMIN");
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value("admin@test.com"))
            .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_asUser_shouldReturn403() throws Exception
    {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isForbidden());
    }

    // --- GET /api/users/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_shouldReturn200() throws Exception
    {
        UserResponse user = new UserResponse(1L, "admin@test.com", "ADMIN");
        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@test.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_whenNotFound_shouldReturn404() throws Exception
    {
        when(userService.getUserById(99L))
            .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    // --- POST /api/users ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_asAdmin_shouldReturn201() throws Exception
    {
        UserResponse response = new UserResponse(2L, "new@test.com", "USER");
        when(userService.createUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@test.com\",\"password\":\"password123\",\"role\":\"USER\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("new@test.com"))
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createUser_asUser_shouldReturn403() throws Exception
    {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@test.com\",\"password\":\"password123\",\"role\":\"USER\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_withInvalidEmail_shouldReturn400() throws Exception
    {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"password\":\"password123\",\"role\":\"USER\"}"))
            .andExpect(status().isBadRequest());
    }

    // --- PUT /api/users/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_asAdmin_shouldReturn200() throws Exception
    {
        UserResponse response = new UserResponse(1L, "updated@test.com", "ADMIN");
        when(userService.updateUser(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"updated@test.com\",\"password\":\"newpass123\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("updated@test.com"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateUser_asUser_shouldReturn403() throws Exception
    {
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"updated@test.com\",\"password\":\"newpass123\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isForbidden());
    }

    // --- DELETE /api/users/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_asAdmin_shouldReturn204() throws Exception
    {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_whenNotFound_shouldReturn404() throws Exception
    {
        doThrow(new ResourceNotFoundException("User not found with id: 99"))
            .when(userService).deleteUser(99L);

        mockMvc.perform(delete("/api/users/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteUser_asUser_shouldReturn403() throws Exception
    {
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isForbidden());
    }
}

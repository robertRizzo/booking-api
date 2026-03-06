package com.booking_api.controller;

import java.util.List;

import com.booking_api.config.SecurityConfig;
import com.booking_api.dto.RoomResponse;
import com.booking_api.exception.ResourceNotFoundException;
import com.booking_api.service.CustomUserDetailsService;
import com.booking_api.service.JwtService;
import com.booking_api.service.RoomService;
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

@WebMvcTest(RoomController.class)
@Import(SecurityConfig.class)
class RoomControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomService roomService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // --- GET /api/rooms ---

    @Test
    void getAllRooms_withoutAuth_shouldReturn401() throws Exception
    {
        mockMvc.perform(get("/api/rooms"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllRooms_asAdmin_shouldReturn200() throws Exception
    {
        RoomResponse room = new RoomResponse(1L, "Room A", 10);
        when(roomService.getAllRooms()).thenReturn(List.of(room));

        mockMvc.perform(get("/api/rooms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Room A"))
            .andExpect(jsonPath("$[0].capacity").value(10));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllRooms_asUser_shouldReturn200() throws Exception
    {
        when(roomService.getAllRooms()).thenReturn(List.of());

        mockMvc.perform(get("/api/rooms"))
            .andExpect(status().isOk());
    }

    // --- GET /api/rooms/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void getRoomById_shouldReturn200() throws Exception
    {
        RoomResponse room = new RoomResponse(1L, "Room A", 10);
        when(roomService.getRoomById(1L)).thenReturn(room);

        mockMvc.perform(get("/api/rooms/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Room A"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getRoomById_whenNotFound_shouldReturn404() throws Exception
    {
        when(roomService.getRoomById(99L))
            .thenThrow(new ResourceNotFoundException("Room not found with id: 99"));

        mockMvc.perform(get("/api/rooms/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Room not found with id: 99"));
    }

    // --- POST /api/rooms ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRoom_asAdmin_shouldReturn201() throws Exception
    {
        RoomResponse response = new RoomResponse(1L, "New Room", 5);
        when(roomService.createRoom(any())).thenReturn(response);

        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Room\",\"capacity\":5}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("New Room"))
            .andExpect(jsonPath("$.capacity").value(5));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createRoom_asUser_shouldReturn403() throws Exception
    {
        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Room\",\"capacity\":5}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRoom_withInvalidData_shouldReturn400() throws Exception
    {
        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"capacity\":-1}"))
            .andExpect(status().isBadRequest());
    }

    // --- PUT /api/rooms/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRoom_asAdmin_shouldReturn200() throws Exception
    {
        RoomResponse response = new RoomResponse(1L, "Updated Room", 12);
        when(roomService.updateRoom(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/rooms/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Room\",\"capacity\":12}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Room"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateRoom_asUser_shouldReturn403() throws Exception
    {
        mockMvc.perform(put("/api/rooms/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Room\",\"capacity\":12}"))
            .andExpect(status().isForbidden());
    }

    // --- DELETE /api/rooms/{id} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRoom_asAdmin_shouldReturn204() throws Exception
    {
        doNothing().when(roomService).deleteRoom(1L);

        mockMvc.perform(delete("/api/rooms/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRoom_whenNotFound_shouldReturn404() throws Exception
    {
        doThrow(new ResourceNotFoundException("Room not found with id: 99"))
            .when(roomService).deleteRoom(99L);

        mockMvc.perform(delete("/api/rooms/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteRoom_asUser_shouldReturn403() throws Exception
    {
        mockMvc.perform(delete("/api/rooms/1"))
            .andExpect(status().isForbidden());
    }
}

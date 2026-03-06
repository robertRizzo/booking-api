package com.booking_api.controller;

import java.time.LocalDateTime;
import java.util.List;

import com.booking_api.config.SecurityConfig;
import com.booking_api.dto.BookingResponse;
import com.booking_api.exception.BookingConflictException;
import com.booking_api.exception.ResourceNotFoundException;
import com.booking_api.service.BookingService;
import com.booking_api.service.CustomUserDetailsService;
import com.booking_api.service.JwtService;
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

@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private static final LocalDateTime FUTURE_START = LocalDateTime.now().plusDays(10);
    private static final LocalDateTime FUTURE_END = FUTURE_START.plusHours(1);

    private BookingResponse sampleBooking()
    {
        return new BookingResponse(
            1L, 1L, "user@test.com", 1L, "Room A",
            FUTURE_START, FUTURE_END, "CONFIRMED");
    }

    private String bookingJson()
    {
        return """
            {
                "userId": 1,
                "roomId": 1,
                "startTime": "%s",
                "endTime": "%s"
            }
            """.formatted(FUTURE_START, FUTURE_END);
    }

    // --- GET /api/bookings ---

    @Test
    void getAllBookings_withoutAuth_shouldReturn401() throws Exception
    {
        mockMvc.perform(get("/api/bookings"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllBookings_asAdmin_shouldReturn200() throws Exception
    {
        when(bookingService.getAllBookings()).thenReturn(List.of(sampleBooking()));

        mockMvc.perform(get("/api/bookings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllBookings_asUser_shouldReturn403() throws Exception
    {
        mockMvc.perform(get("/api/bookings"))
            .andExpect(status().isForbidden());
    }

    // --- GET /api/bookings/my ---

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void getMyBookings_asUser_shouldReturn200() throws Exception
    {
        when(bookingService.getBookingsByUserEmail("user@test.com"))
            .thenReturn(List.of(sampleBooking()));

        mockMvc.perform(get("/api/bookings/my"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userEmail").value("user@test.com"));
    }

    // --- GET /api/bookings/{id} ---

    @Test
    @WithMockUser(roles = "USER")
    void getBookingById_shouldReturn200() throws Exception
    {
        when(bookingService.getBookingById(1L)).thenReturn(sampleBooking());

        mockMvc.perform(get("/api/bookings/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getBookingById_whenNotFound_shouldReturn404() throws Exception
    {
        when(bookingService.getBookingById(99L))
            .thenThrow(new ResourceNotFoundException("Booking not found with id: 99"));

        mockMvc.perform(get("/api/bookings/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Booking not found with id: 99"));
    }

    // --- POST /api/bookings ---

    @Test
    @WithMockUser(roles = "USER")
    void createBooking_asUser_shouldReturn201() throws Exception
    {
        when(bookingService.createBooking(any())).thenReturn(sampleBooking());

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBooking_conflict_shouldReturn409() throws Exception
    {
        when(bookingService.createBooking(any()))
            .thenThrow(new BookingConflictException("Room is already booked during this time"));

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Room is already booked during this time"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createBooking_withMissingFields_shouldReturn400() throws Exception
    {
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": null}"))
            .andExpect(status().isBadRequest());
    }

    // --- PUT /api/bookings/{id} ---

    @Test
    @WithMockUser(roles = "USER")
    void updateBooking_shouldReturn200() throws Exception
    {
        when(bookingService.updateBooking(eq(1L), any())).thenReturn(sampleBooking());

        mockMvc.perform(put("/api/bookings/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    // --- DELETE /api/bookings/{id} ---

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void cancelBooking_asUser_shouldReturn204() throws Exception
    {
        doNothing().when(bookingService).cancelBooking(1L, "user@test.com");

        mockMvc.perform(delete("/api/bookings/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void cancelBooking_whenNotFound_shouldReturn404() throws Exception
    {
        doThrow(new ResourceNotFoundException("Booking not found with id: 99"))
            .when(bookingService).cancelBooking(99L, "user@test.com");

        mockMvc.perform(delete("/api/bookings/99"))
            .andExpect(status().isNotFound());
    }
}

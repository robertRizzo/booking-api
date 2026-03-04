package com.booking_api.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
    @NotNull(message = "User ID is required")
    Long userId,

    @NotNull(message = "Room ID is required")
    Long roomId,

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    LocalDateTime startTime,

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    LocalDateTime endTime
)
{}

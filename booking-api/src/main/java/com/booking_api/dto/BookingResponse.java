package com.booking_api.dto;

import java.time.LocalDateTime;

public record BookingResponse(
    Long id,
    Long userId,
    String userEmail,
    Long roomId,
    String roomName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String status
)
{}

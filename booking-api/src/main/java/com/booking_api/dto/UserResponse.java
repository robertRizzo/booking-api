package com.booking_api.dto;

public record UserResponse(
    Long id,
    String email,
    String role
)
{}

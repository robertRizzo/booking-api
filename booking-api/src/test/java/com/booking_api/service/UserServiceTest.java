package com.booking_api.service;

import com.booking_api.dto.UserRequest;
import com.booking_api.dto.UserResponse;
import com.booking_api.exception.ResourceNotFoundException;
import com.booking_api.model.Role;
import com.booking_api.model.User;
import com.booking_api.repository.UserRepository;
import com.booking_api.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest
{
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // --- getAllUsers ---

    @Test
    void getAllUsers_shouldReturnList()
    {
        User user = TestDataFactory.createDefaultUser();
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("test@test.com", result.get(0).email());
    }

    // --- getUserById ---

    @Test
    void getUserById_shouldReturnUser()
    {
        User user = TestDataFactory.createDefaultUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserById(1L);

        assertEquals(1L, result.id());
        assertEquals("test@test.com", result.email());
        assertEquals("USER", result.role());
    }

    @Test
    void getUserById_shouldThrowWhenNotFound()
    {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> userService.getUserById(99L));
    }

    // --- createUser ---

    @Test
    void createUser_shouldDefaultToUserRole()
    {
        UserRequest request = TestDataFactory.createUserRequest(
            "new@test.com", "password123", null);
        User saved = TestDataFactory.createUser(1L, "new@test.com", Role.USER);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse result = userService.createUser(request);

        assertEquals("USER", result.role());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldParseAdminRole()
    {
        UserRequest request = TestDataFactory.createUserRequest(
            "admin@new.com", "password123", "ADMIN");
        User saved = TestDataFactory.createUser(1L, "admin@new.com", Role.ADMIN);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse result = userService.createUser(request);

        assertEquals("ADMIN", result.role());
    }

    // --- updateUser ---

    @Test
    void updateUser_shouldUpdateAndReturnResponse()
    {
        User existing = TestDataFactory.createUser(1L, "old@test.com", Role.USER);
        User updated = TestDataFactory.createUser(1L, "updated@test.com", Role.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(updated);

        UserRequest request = TestDataFactory.createUserRequest(
            "updated@test.com", "newpass", "ADMIN");
        UserResponse result = userService.updateUser(1L, request);

        assertEquals("updated@test.com", result.email());
        assertEquals("ADMIN", result.role());
        verify(userRepository).save(existing);
    }

    @Test
    void updateUser_shouldThrowWhenNotFound()
    {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UserRequest request = TestDataFactory.createUserRequest(
            "any@test.com", "password", "USER");

        assertThrows(ResourceNotFoundException.class,
            () -> userService.updateUser(99L, request));

        verify(userRepository, never()).save(any());
    }

    // --- deleteUser ---

    @Test
    void deleteUser_shouldDeleteWhenExists()
    {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_shouldThrowWhenNotFound()
    {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
            () -> userService.deleteUser(99L));

        verify(userRepository, never()).deleteById(any());
    }
}
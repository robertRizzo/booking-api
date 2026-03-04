package com.booking_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.booking_api.dto.UserRequest;
import com.booking_api.dto.UserResponse;
import com.booking_api.model.Role;
import com.booking_api.model.User;
import com.booking_api.repository.UserRepository;

@Service
public class UserService
{
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository)
    {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsers()
    {
        return userRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    public UserResponse getUserById(Long id)
    {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        return toResponse(user);
    }

    public UserResponse createUser(UserRequest request)
    {
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setRole(parseRole(request.role()));

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public UserResponse updateUser(Long id, UserRequest request)
    {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        
        existing.setEmail(request.email());
        existing.setPassword(request.password());
        existing.setRole(parseRole(request.role()));

        User saved = userRepository.save(existing);
        return toResponse(saved);
    }

    // deleteUser
    public void deleteUser(Long id)
    {
        if(!userRepository.existsById(id))
        {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private Role parseRole(String role)
    {
        if(role == null || role.isBlank())
        {
            return Role.USER;
        }
        try
        {
            return Role.valueOf(role.toUpperCase());
        }
        catch(IllegalArgumentException e)
        {
            throw new IllegalArgumentException("Invalid role: " + role + ". Must be USER or ADMIN");
        }
    }

    private UserResponse toResponse(User user)
    {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name());
    }
}

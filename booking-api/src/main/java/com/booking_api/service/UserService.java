package com.booking_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

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

    // getAllUSers
    public List<User> getAllUsers()
    {
        return userRepository.findAll();
    }

    public User getUserById(Long id)
    {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    // createUser
    public User createUser(User user)
    {
        return userRepository.save(user);
    }

    // updateUSer
    public User updateUser(Long id, User userDetails)
    {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        
            existing.setEmail(userDetails.getEmail());
            existing.setPassword(userDetails.getPassword());
            existing.setRole(userDetails.getRole());
        return userRepository.save(existing);
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
}

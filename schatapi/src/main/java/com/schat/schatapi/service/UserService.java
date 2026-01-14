package com.schat.schatapi.service;

import com.schat.schatapi.dto.UserProfileResponse;
import com.schat.schatapi.dto.UpdateProfileRequest;
import com.schat.schatapi.model.User;
import com.schat.schatapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;  // Same repo used by UserDetailsServiceImpl ..
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("❌Error: User not found.."));
        
        return mapToProfileResponse(user);
    }

    public UserProfileResponse updateUserProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("❌Error: User not found.."));
        
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (userRepository.existsByEmail(request.getEmail()) && 
                !user.getEmail().equals(request.getEmail())) {
                throw new RuntimeException("❌Error: Email is already in use!!..");
            }
            user.setEmail(request.getEmail());
        }
        
        if (request.getCurrentPassword() != null && request.getNewPassword() != null) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("❌Error: Current password is incorrect..");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        return mapToProfileResponse(user);
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        // Convert full User entity to response DTO..
        Set<String> roles = user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet());

        return new UserProfileResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            roles,
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}

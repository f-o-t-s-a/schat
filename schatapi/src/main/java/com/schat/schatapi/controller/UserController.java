package com.schat.schatapi.controller;

import com.schat.schatapi.dto.*;
import com.schat.schatapi.model.User;
import com.schat.schatapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            // Getting authenticated user from security context ..
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Fetching user profile..
            UserProfileResponse profile = userService.getUserProfile(username);

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("❌Error: " + e.getMessage()));
        }
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<?> getUserProfileByUsername(@PathVariable String username) {
        try {
            UserProfileResponse profile = userService.getUserProfile(username);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("❌Error: User not found.."));
        }
    }
    
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@Valid @RequestBody UpdateProfileRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            UserProfileResponse updatedProfile = userService.updateUserProfile(username, request);
            
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("❌Error: " + e.getMessage()));
        }
    }
}

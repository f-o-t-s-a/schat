package com.schat.schatapi.controller;

import com.schat.schatapi.dto.*;
import com.schat.schatapi.model.User;
import com.schat.schatapi.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
//import java.util.HashSet;
//import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse response = authService.authenticateUser(
                loginRequest.getUsername(), loginRequest.getPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("❌Error: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
          /** / Handling roles assignment..
          Set<String> strRoles = signUpRequest.getRoles();
          Set<Role> roles = new HashSet<>();

          if (strRoles == null || strRoles.isEmpty()) {
            // Default role: USER
            Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
          } else {
            strRoles.forEach(role -> {
              switch (role) {
                case "ROLE_ADMIN":
                  Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                  roles.add(adminRole);
                  break;
                case "ROLE_MODERATOR":
                  Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                  roles.add(modRole);
                  break;
                default:
                  Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                  roles.add(userRole);
              }
            });
          }*/

          User user = authService.registerUser(
            signUpRequest.getUsername(),
            signUpRequest.getEmail(),
            signUpRequest.getPassword(),
            signUpRequest.getRoles());
        
          return ResponseEntity.ok(new MessageResponse(signUpRequest.getUsername() + " registered successfully!!.."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshtoken(@Valid @RequestBody TokenRefreshRequest request) {
        try {
            JwtResponse response = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("❌Error: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        try {
            authService.logoutUser();
            return ResponseEntity.ok(new MessageResponse("🎉 Log out successful!!.."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("❌ Error: " + e.getMessage()));
        }
    }
}


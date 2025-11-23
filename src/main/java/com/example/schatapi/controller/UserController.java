package com.example.schatapi.controller;

import com.example.schatapi.dto.AuthResponse;
import com.example.schatapi.dto.LoginRequest;
import com.example.schatapi.model.User;
import com.example.schatapi.repository.UserRepository;
import com.example.schatapi.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if(userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(user.getUsername() + " already exists..");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser.getUsername());
	return ResponseEntity.ok(new AuthResponse(token, savedUser.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            String token = jwtUtil.generateToken(loginRequest.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, loginRequest.getUsername()));
        } catch(BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password..");
        }
    }
}

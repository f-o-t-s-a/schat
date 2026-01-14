package com.schat.schatapi.service;

import com.schat.schatapi.dto.JwtResponse;
import com.schat.schatapi.model.RefreshToken;
import com.schat.schatapi.model.User;
import com.schat.schatapi.repository.UserRepository;
import com.schat.schatapi.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private ThresholdTokenService thresholdTokenService;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenService refreshTokenService;

    public JwtResponse authenticateUser(String username, String password) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate UNSIGNED JWT (header.payload only)
        String unsignedJwt = jwtUtils.generateUnsignedJwtToken(authentication);
        
        // Sign with threshold signature: header.payload::threshold_signature
        String signedToken = thresholdTokenService.signToken(unsignedJwt);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        // Generate refresh token
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getId())
            .getToken();

        return new JwtResponse(
            signedToken,  // Returns: header.payload::threshold_signature
            userDetails.getId(),
            userDetails.getUsername(),
            userDetails.getEmail(),
            roles,
            refreshToken
        );
    }


    public User registerUser(String username, String email, String password, Set<String> strRoles) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("❌Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("❌Error: Email is already in use!");
        }

        // Creating new user's account..
        User user = new User(username, email, encoder.encode(password));

        // Assigning party index for threshold signing (round-robin assignment)..
        Long userCount = userRepository.count();
        int partyIndex = (userCount.intValue() % 3) + 1; // Assuming 3 parties..
        user.setPartyIndex(partyIndex);

        userRepository.save(user);
        return user;
    }

    public JwtResponse refreshToken(String requestRefreshToken) {
        return refreshTokenService.findByToken(requestRefreshToken)
            .map(refreshTokenService::verifyExpiration)
            .map(RefreshToken::getUser)
            .map(user -> {
                String token = jwtUtils.generateJwtToken(user.getUsername());
                return new JwtResponse(token, user.getId(), user.getUsername(),
                                     user.getEmail(), List.of("ROLE_USER"),
                                     requestRefreshToken);
            })
            .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    public void logoutUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        refreshTokenService.deleteByUserId(userId);
    }
}

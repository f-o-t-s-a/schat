package com.schat.schatapi.security;

import com.schat.schatapi.service.UserDetailsServiceImpl;
import com.schat.schatapi.service.ThresholdTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    
    static {
        System.out.println("========================================");
        System.out.println("AuthTokenFilter CLASS LOADED");
        System.out.println("========================================");
    }
    
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    @Autowired
    private ThresholdTokenService thresholdTokenService;
    
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
    
    public AuthTokenFilter() {
        System.out.println("========================================");
        System.out.println("AuthTokenFilter CONSTRUCTOR CALLED");
        System.out.println("========================================");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = parseJwt(request);
            
            if (token != null && token.contains("::")) {
                logger.debug("🔐 Validating threshold-signed token");
                
                // Step 1: Verify threshold signature (PRIMARY authentication)
                boolean signatureValid = thresholdTokenService.verifyTokenSignature(token);
                
                if (!signatureValid) {
                    logger.error("❌ Threshold signature verification FAILED..");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"❌ Invalid threshold signature..\"}");
                    return;  // Reject request
                }
                
                logger.debug("✅ Threshold signature verified");
                
                // Step 2: Validate JWT structure and expiration
                if (!jwtUtils.validateJwtStructure(token)) {
                    logger.error("❌ JWT structure validation failed");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Invalid token structure\"}");
                    return;
                }
                
                logger.debug("✅ JWT structure validated");
                
                // Step 3: Extract username and set authentication
                String username = jwtUtils.getUserNameFromJwtToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                logger.info("✅ User '{}' authenticated via threshold signature", username);
            } else {
                logger.warn("⚠️ Token missing or invalid format (no :: separator)");
            }
        } catch (Exception e) {
            logger.error("❌ Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        
        logger.debug("Authorization header: {}", headerAuth != null ? "present" : "null");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            String token = headerAuth.substring(7);
            logger.debug("✓ Token extracted from Bearer header");
            return token;
        }

        return null;
    }
}

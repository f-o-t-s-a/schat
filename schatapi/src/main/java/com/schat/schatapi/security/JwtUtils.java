package com.schat.schatapi.security;

import com.schat.schatapi.service.ThresholdTokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import com.schat.schatapi.service.UserDetailsImpl;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${schat.app.jwtSecret}")
    private String jwtSecret;

    @Value("${schat.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${schat.app.jwtRefreshExpirationMs}")
    private int jwtRefreshExpirationMs;

    @Autowired
    private ThresholdTokenService thresholdTokenService;

    private SecretKey getSigningKey() {
        // Creating a proper SecretKey from the secret string..
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Generate UNSIGNED JWT (header.payload only, no signature)
     * Threshold signature will be added separately
     */
    public String generateUnsignedJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        // Build JWT without signing it
        String unsignedJwt = Jwts.builder()
            .setSubject(userPrincipal.getUsername())
            .claim("email", userPrincipal.getEmail())
            .claim("id", userPrincipal.getId())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .compact();  // This creates header.payload (no signature)

        logger.debug("Generated unsigned JWT for user: {}", userPrincipal.getUsername());
        return unsignedJwt;
    }
    
    public String generateJwtToken(String username) {
      try {
        logger.info("Starting JWT generation for user: {}", username);
        
        // Using threshold signature to sign the token..
        String unsignedToken = Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
        
        logger.info("Unsigned token created successfully");
        logger.debug("Unsigned token: {}", unsignedToken);
        
        String signedToken = thresholdTokenService.signToken(unsignedToken);
        logger.info("Token signed successfully with threshold signature");
        
        return signedToken;
      } catch(Exception e) {
        logger.error("❌JWT generation failed: {}", e.getMessage(), e);
        throw new RuntimeException("Token signing failed: " + e.getMessage(), e);
      }
    }

    public String generateRefreshToken(String username) {
        String unsignedToken = Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtRefreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
        
        return thresholdTokenService.signToken(unsignedToken);
    }

    public boolean validateJwtToken(String authToken) {
        try {
            logger.info("=== JWT Validation Started ===");
            logger.debug("Token contains '::': {}", authToken.contains("::"));
        
            // First verifying threshold signature..
            if (authToken.contains("::")) {
                logger.info("Verifying threshold signature...");
                boolean thresholdValid = thresholdTokenService.verifyTokenSignature(authToken);
                logger.info("Threshold signature validation: {}", thresholdValid ? "VALID ✓" : "INVALID ✗");
                
                if (!thresholdValid) {
                    logger.error("❌ Threshold signature verification failed..");
                    return false;
                }
            }
            
            // Extractimg the unsigned token..
            logger.info("Validating JWT structure...");
            logger.info("Extracting unsigned JWT token...");
            String unsignedToken = thresholdTokenService.extractUnsignedToken(authToken);
            logger.debug("Unsigned token length: {}", unsignedToken.length());
            logger.debug("Unsigned token preview: {}...", unsignedToken.substring(0, Math.min(50, unsignedToken.length())));
        
            // Then validating JWT structure..
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(unsignedToken);
        
            logger.info("=== JWT Validation: SUCCESS ✓✓✓ ===");
            return true;
        } catch (SecurityException e) {
            logger.error("❌ Invalid threshold signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("❌ Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("❌ JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("❌ JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("❌ JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Unexpected error: {}", e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Extract username from token (works with both signed and unsigned)
     */
    public String getUserNameFromJwtToken(String token) {
        // Remove threshold signature if present
        String jwtPart = token.split("::")[0];
        
        logger.info("⚠️ Extracting username from token...");
        // Parsing without validation (we'll validate via threshold signature)..
        return Jwts.parser()
            .build()
            .parseClaimsJwt(jwtPart)  // Use parseClaimsJwt for unsigned tokens..
            .getBody()
            .getSubject();
        
        /*/ Verifying threshold signature before extracting claims..
        if (!thresholdTokenService.verifyTokenSignature(token)) {
            throw new SecurityException("❌ Invalid token signature..");
        }
        
    
        // Extracting the unsigned token (remove threshold signature)..
        String unsignedToken = thresholdTokenService.extractUnsignedToken(token);
        logger.debug("Unsigned token for username extraction: {}...", unsignedToken.substring(0, Math.min(50, unsignedToken.length())));
        
        String username = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(unsignedToken)
                .getPayload()
                .getSubject();
        
        logger.info("✓ Username extracted: {}", username);
        return username;*/
    }
    
    /**
     * Getting all claims from token..
     */
    public Claims getClaimsFromToken(String token) {
        String jwtPart = token.split("::")[0];
        
        return Jwts.parser()
            .build()
            .parseClaimsJwt(jwtPart)
            .getBody();
    }
    
    /**
     * Checking if token is expired..
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * Validate token structure (not cryptographic validation)
     */
    public boolean validateJwtStructure(String token) {
        try {
            String jwtPart = token.split("::")[0];
            
            // Checking basic JWT structure..
            String[] parts = jwtPart.split("\\.");
            if (parts.length != 2 && parts.length != 3) {
                logger.warn("⚠️ Invalid JWT structure: expected 2-3 parts, got {}", parts.length);
                return false;
            }

            // Parsing claims..
            Claims claims = getClaimsFromToken(token);
            
            // Checking expiration
            if (isTokenExpired(token)) {
                logger.warn("❌ JWT token is expired");
                return false;
            }

            return true;
        } catch (MalformedJwtException e) {
            logger.error("❌ Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("❌ JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("❌JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("❌ JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}

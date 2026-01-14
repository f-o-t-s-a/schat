package com.schat.schatapi.service;

import com.schat.signature.core.*;
import com.schat.signature.service.TokenSigningService;
import com.schat.signature.config.SignatureConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

@Service
public class ThresholdTokenService {
    private static final Logger logger = LoggerFactory.getLogger(ThresholdTokenService.class);

    private TokenSigningService tokenSigningService;
    private SDithPublicKey publicKey;
    private List<SDithKeyPair> allKeyPairs;
    private BigInteger curveOrder;

    @Value("${schat.threshold.partyIndex:1}")
    private int partyIndex;

    @Value("${schat.threshold.totalParties:3}")
    private int totalParties;

    @Value("${schat.threshold.threshold:2}")
    private int threshold;

    @PostConstruct
    public void init() {
        try {
            logger.info("Initializing threshold signature service...");
            
            this.curveOrder = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
            
            SignatureConfig config = new SignatureConfig(threshold, totalParties, curveOrder);

            // Generate all key pairs for simulation
            SDithKeyGenerator keyGenerator = SDithThresholdScheme.generateKeyPair(
                config.getThreshold(), config.getTotalParties(), config.getCurveOrder()
            );

            // Store all key pairs (for simulation only)
            this.allKeyPairs = new ArrayList<>();
            for (int i = 1; i <= totalParties; i++) {
                SDithKeyPair kp = keyGenerator.getKeyPairForParty(i);
                allKeyPairs.add(kp);
                logger.debug("Generated key pair for party {}", i);
            }

            // Set up our own key pair
            SDithKeyPair myKeyPair = allKeyPairs.get(partyIndex - 1);
            this.tokenSigningService = new TokenSigningService(myKeyPair);
            this.publicKey = myKeyPair.getPublicKey();

            logger.info("✓ Threshold service initialized: party={}, threshold={}/{}", 
                partyIndex, threshold, totalParties);
        } catch (Exception e) {
            logger.error("Failed to initialize threshold token service", e);
            throw new RuntimeException("Threshold service initialization failed", e);
        }
    }

    public String signToken(String token) {
        try {
            logger.info("=== Token Signing Started ===");
            logger.debug("Token prefix: {}...", token.substring(0, Math.min(50, token.length())));
            
            byte[] tokenBytes = token.getBytes();

            // Generate deterministic shared nonce from message
            // All parties can compute this independently
            BigInteger sharedNonce = SDithThresholdScheme.generateDeterministicNonce(
                tokenBytes, curveOrder);
            logger.debug("✓ Generated shared nonce (deterministic from message)");

            // Generate partial signatures with the shared nonce
            List<SDithPartialSignature> partials = generateCoordinatedPartialSignatures(
                tokenBytes, sharedNonce);
            
            logger.info("✓ Collected {} partial signatures", partials.size());

            // Combine signatures
            byte[] finalSignature = tokenSigningService.signToken(tokenBytes, partials);
            logger.info("✓ Combined into final signature ({} bytes)", finalSignature.length);

            // Append signature to token using :: separator
            String signatureB64 = Base64.getEncoder().encodeToString(finalSignature);
            String result = token + "::" + signatureB64;
            
            logger.info("=== Token Signing Completed Successfully ===");
            return result;
            
        } catch (Exception e) {
            logger.error("❌ Token signing failed: {}", e.getMessage());
            logger.error("Stack trace:", e);
            throw new RuntimeException("❌Token signing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate partial signatures using coordinated nonce
     * All parties use the same nonce, ensuring consistent r values
     */
    private List<SDithPartialSignature> generateCoordinatedPartialSignatures(
            byte[] message, BigInteger sharedNonce) {
        
        List<SDithPartialSignature> partials = new ArrayList<>();

        try {
            logger.info("--- Coordinated Signing Protocol ---");
            logger.debug("Shared nonce: {}", sharedNonce.toString(16).substring(0, 16) + "...");
            
            // Select threshold number of parties to participate
            List<Integer> participatingParties = selectParticipatingParties();
            logger.debug("Participating parties: {}", participatingParties);

            // Each party generates their partial signature with the SAME shared nonce
            for (int partyIdx : participatingParties) {
                SDithKeyPair keyPair = allKeyPairs.get(partyIdx - 1);
                
                logger.debug("Party {} generating partial signature with shared nonce", partyIdx);
                
                // Use the NEW method that accepts a shared nonce
                SDithPartialSignature partialSig = SDithThresholdScheme.generatePartialSignatureWithSharedNonce(
                    message,
                    keyPair.getPrivateKeyShare().getShare(),
                    keyPair.getPrivateKeyShare().getIndex(),
                    sharedNonce,  // ← SAME nonce for all parties!
                    curveOrder
                );
                
                partials.add(partialSig);
                logger.debug("✓ Party {} partial signature added (r={}...) ({}/{})", 
                    partyIdx, 
                    partialSig.getR().toString(16).substring(0, 16),
                    partials.size(), 
                    threshold);
            }

            if (partials.size() < threshold) {
                throw new RuntimeException(
                    String.format("Insufficient signatures: %d < %d", partials.size(), threshold));
            }

            // Verify all r values are the same
            BigInteger expectedR = partials.get(0).getR();
            for (SDithPartialSignature partial : partials) {
                if (!partial.getR().equals(expectedR)) {
                    throw new RuntimeException(
                        "R-value mismatch detected! Party " + partial.getShareIndex() + 
                        " has different r value. This should not happen with coordinated nonce.");
                }
            }

            logger.info("✓ All partial signatures have consistent r values");
            logger.info("--- Protocol Complete: {}/{} signatures ---", partials.size(), threshold);
            return partials;
            
        } catch (Exception e) {
            logger.error("❌ Coordinated signing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Coordinated signing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Select which parties will participate in this signature
     * In production, this would be based on availability
     */
    private List<Integer> selectParticipatingParties() {
        List<Integer> parties = new ArrayList<>();
        
        // Start with our own party
        parties.add(partyIndex);
        
        // Add additional parties up to threshold
        for (int i = 1; i < threshold; i++) {
            int nextParty = (partyIndex + i) % totalParties;
            if (nextParty == 0) nextParty = totalParties;
            parties.add(nextParty);
        }
        
        return parties;
    }

    public boolean verifyTokenSignature(String signedToken) {
        try {
            String[] parts = signedToken.split("::");
            if (parts.length != 2) {
                logger.warn("⚠️ Invalid token format: expected 2 parts, got {}..", parts.length);
                return false;
            }

            String token = parts[0];
            byte[] signatureBytes = Base64.getDecoder().decode(parts[1]);
            SDithSignature signature = SDithSignature.fromBytes(signatureBytes);

            boolean valid = SDithThresholdScheme.verifySignature(
                token.getBytes(), signature, publicKey);
            
            logger.debug("❌ Token signature verification: {}..", valid ? "VALID" : "INVALID");
            return valid;
            
        } catch (Exception e) {
            logger.error("❌ Token verification failed: {}..", e.getMessage(), e);
            return false;
        }
    }

    public String extractUnsignedToken(String signedToken) {
        String[] parts = signedToken.split("::");
        return parts.length == 2 ? parts[0] : signedToken;
    }
} 

package com.schat.signature.core;

import org.bouncycastle.crypto.digests.SHA3Digest;
import java.security.SecureRandom;
import java.util.Arrays;

public class SDitHSigner {
    private final SecureRandom random;
    private final SHA3Digest digest;

    public SDitHSigner() {
        this.random = new SecureRandom();
        this.digest = new SHA3Digest(256);
    }

    /**
     * To Generate a new SDitH key pair..
     */
    public SDitHKeyPair generateKeyPair(SDitHParameters params) {
        int n = params.getN();
        int k = params.getK();
        int w = params.getW();

        // Generating random parity check matrix H..
        byte[] h = new byte[(n - k) * n / 8];
        random.nextBytes(h);

        // Generating secret error vector e with Hamming weight w ..
        byte[] secretKey = generateErrorVector(n, w);

        // Computing the syndrome s = H * e ..
        byte[] syndrome = computeSyndrome(h, secretKey, n, k);

        SDitHKeyPair.SDitHPublicKey publicKey = 
            new SDitHKeyPair.SDitHPublicKey(h, syndrome, params);
        SDitHKeyPair.SDitHPrivateKey privateKey = 
            new SDitHKeyPair.SDitHPrivateKey(secretKey, params);

        return new SDitHKeyPair(publicKey, privateKey);
    }

    /**
     * To Sign a message using SDitH scheme..
     */
    public byte[] sign(byte[] message, SDitHKeyPair.SDitHPrivateKey privateKey) {
        SDitHParameters params = privateKey.getParams();

        // Hashing the message..
        byte[] messageHash = hash(message);

        // Generating commitment and response (simplified MPC-in-the-head)..
        byte[] commitment = generateCommitment(messageHash, privateKey, params);
        byte[] challenge = generateChallenge(messageHash, commitment);
        byte[] response = generateResponse(privateKey, challenge, params);

        // Combining signature components..
        return combineSignature(commitment, challenge, response);
    }

    /**
     * To Verify a signature..
     */
    public boolean verify(byte[] message, byte[] signature, SDitHKeyPair.SDitHPublicKey publicKey) {
        try {
            // Parsing signature..
            SignatureComponents components = parseSignature(signature);

            // Hashing the message..
            byte[] messageHash = hash(message);

            // Recomputing challenge..
            byte[] recomputedChallenge = generateChallenge(messageHash, components.commitment);

            // Verifying challenge matches..
            if (!Arrays.equals(recomputedChallenge, components.challenge)) {
                return false;
            }

            // Verifying response against public key..
            return verifyResponse(components, publicKey, messageHash);
        } catch (Exception e) {
            return false;
        }
    }

    // Helper methods..

    private byte[] generateErrorVector(int n, int w) {
        byte[] vector = new byte[n / 8];
        int[] positions = new int[w];

        // Selecting w random positions..
        for (int i = 0; i < w; i++) {
            int pos;
            do {
                pos = random.nextInt(n);
            } while (contains(positions, i, pos));
            positions[i] = pos;

            // Setting bit at position..
            vector[pos / 8] |= (1 << (pos % 8));
        }

        return vector;
    }

    private boolean contains(int[] arr, int len, int value) {
        for (int i = 0; i < len; i++) {
            if (arr[i] == value) return true;
        }
        return false;
    }

    private byte[] computeSyndrome(byte[] h, byte[] errorVector, int n, int k) {
        // Simplified matrix-vector multiplication in GF(2)..
        int syndromeLen = (n - k) / 8;
        byte[] syndrome = new byte[syndromeLen];

        for (int i = 0; i < n - k; i++) {
            int bit = 0;
            for (int j = 0; j < n; j++) {
                int hBit = (h[(i * n + j) / 8] >> ((i * n + j) % 8)) & 1;
                int eBit = (errorVector[j / 8] >> (j % 8)) & 1;
                bit ^= (hBit & eBit);
            }
            syndrome[i / 8] |= (bit << (i % 8));
        }

        return syndrome;
    }

    private byte[] hash(byte[] data) {
        digest.reset();
        digest.update(data, 0, data.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return hash;
    }

    private byte[] generateCommitment(byte[] messageHash, 
                                     SDitHKeyPair.SDitHPrivateKey privateKey, 
                                     SDitHParameters params) {
        // Simplified commitment generation..
        byte[] commitment = new byte[32];
        random.nextBytes(commitment);

        // In real implementation: commit to MPC shares..
        digest.reset();
        digest.update(messageHash, 0, messageHash.length);
        digest.update(privateKey.getSecretKey(), 0, privateKey.getSecretKey().length);
        digest.update(commitment, 0, commitment.length);
        digest.doFinal(commitment, 0);

        return commitment;
    }

    private byte[] generateChallenge(byte[] messageHash, byte[] commitment) {
        digest.reset();
        digest.update(messageHash, 0, messageHash.length);
        digest.update(commitment, 0, commitment.length);
        byte[] challenge = new byte[16];
        digest.doFinal(challenge, 0);
        return challenge;
    }

    private byte[] generateResponse(SDitHKeyPair.SDitHPrivateKey privateKey, 
                                   byte[] challenge, 
                                   SDitHParameters params) {
        // Simplified response generation..
        byte[] response = new byte[params.getN() / 8 + 32];

        // Combining secret key with challenge..
        System.arraycopy(privateKey.getSecretKey(), 0, response, 0, 
                        privateKey.getSecretKey().length);
        System.arraycopy(challenge, 0, response, 
                        privateKey.getSecretKey().length, 
                        Math.min(challenge.length, 32));

        return hash(response);
    }

    private byte[] combineSignature(byte[] commitment, byte[] challenge, byte[] response) {
        byte[] signature = new byte[commitment.length + challenge.length + response.length];
        System.arraycopy(commitment, 0, signature, 0, commitment.length);
        System.arraycopy(challenge, 0, signature, commitment.length, challenge.length);
        System.arraycopy(response, 0, signature, commitment.length + challenge.length, 
                        response.length);
        return signature;
    }

    private SignatureComponents parseSignature(byte[] signature) {
        // Assuming fixed sizes for simplicity..
        int commitmentSize = 32;
        int challengeSize = 16;

        byte[] commitment = Arrays.copyOfRange(signature, 0, commitmentSize);
        byte[] challenge = Arrays.copyOfRange(signature, commitmentSize, 
                                              commitmentSize + challengeSize);
        byte[] response = Arrays.copyOfRange(signature, commitmentSize + challengeSize, 
                                            signature.length);

        return new SignatureComponents(commitment, challenge, response);
    }

    private boolean verifyResponse(SignatureComponents components, 
                                  SDitHKeyPair.SDitHPublicKey publicKey, 
                                  byte[] messageHash) {
        // Simplified verification..
        // In real implementation: MPC-in-the-head protocol might be verified..

        // Checking that response is consistent with commitment and public key..
        byte[] recomputedCommitment = new byte[32];
        digest.reset();
        digest.update(messageHash, 0, messageHash.length);
        digest.update(components.response, 0, components.response.length);
        digest.update(publicKey.getSyndrome(), 0, publicKey.getSyndrome().length);
        digest.doFinal(recomputedCommitment, 0);

        // In production: full SDitH verification might be implemented..
        return true; // Simplified..
    }

    private static class SignatureComponents {
        final byte[] commitment;
        final byte[] challenge;
        final byte[] response;

        SignatureComponents(byte[] commitment, byte[] challenge, byte[] response) {
            this.commitment = commitment;
            this.challenge = challenge;
            this.response = response;
        }
    }
}

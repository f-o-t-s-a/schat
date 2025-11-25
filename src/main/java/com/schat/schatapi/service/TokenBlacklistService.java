package com.schat.schatapi.service;

import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

	// In-memory storage (Redis to be used in production)..
	private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

	public void blacklistToken(String token) {
		blacklistedTokens.add(token);
	}

	public boolean isBlacklisted(String token) {
		return blacklistedTokens.contains(token);
	}

	public void removeToken(String token) {
		blacklistedTokens.remove(token);
	}
}

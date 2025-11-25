package com.schat.schatapi.controller;

import com.schat.schatapi.service.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

	@Autowired
	private TokenBlacklistService tokenBlacklistService;

	@PostMapping("/logout")
	public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
		// With JWT, logout is primarily handled on client-side, This is Server-side logout..
		// The client should first delete the token from storage..
		if(authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			tokenBlacklistService.blacklistToken(token);
			return ResponseEntity.ok().body("Logout Successful.. Token has been invalidated..");
		}
		return ResponseEntity.badRequest().body("No token provided..");
	}
}

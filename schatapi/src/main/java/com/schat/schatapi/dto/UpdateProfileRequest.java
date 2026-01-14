package com.schat.schatapi.dto;

import jakarta.validation.constraints.Email;

public class UpdateProfileRequest {
    @Email
    private String email;

    private String currentPassword;
    private String newPassword;

    // Getters and Setters..
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}

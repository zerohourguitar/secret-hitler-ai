package com.secrethitler.ai.dtos;

public class LoginRequest {
	private final String username;
    private final String password;

    public LoginRequest(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
}

package com.edificio.app.service;

import com.edificio.app.api.dto.LoginRequest;
import com.edificio.app.api.dto.LoginResponse;
import com.edificio.app.api.dto.RefreshTokenRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}

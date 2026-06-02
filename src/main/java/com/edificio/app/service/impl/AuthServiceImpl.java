package com.edificio.app.service.impl;

import com.edificio.app.api.dto.LoginRequest;
import com.edificio.app.api.dto.LoginResponse;
import com.edificio.app.api.dto.RefreshTokenRequest;
import com.edificio.app.domain.AppUser;
import com.edificio.app.domain.RefreshToken;
import com.edificio.app.exception.InvalidTokenException;
import com.edificio.app.repository.AppUserRepository;
import com.edificio.app.repository.RefreshTokenRepository;
import com.edificio.app.service.AuthService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder refreshTokenJwtDecoder;

    @Value("${app.security.jwt-expiration-minutes}")
    private long accessTokenExpirationMinutes;

    @Value("${app.security.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            AppUserRepository appUserRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtEncoder jwtEncoder,
            @Qualifier("refreshTokenJwtDecoder") JwtDecoder refreshTokenJwtDecoder
    ) {
        this.authenticationManager = authenticationManager;
        this.appUserRepository = appUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenJwtDecoder = refreshTokenJwtDecoder;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        AppUser user = findActiveUser(authentication.getName());
        return buildTokenResponse(user);
    }

    @Override
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public LoginResponse refresh(RefreshTokenRequest request) {
        Jwt jwt = decode(request.refreshToken());
        validateRefreshJwt(jwt);

        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token no reconocido"));
        validateStoredRefreshToken(storedToken);
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        AppUser user = findActiveUser(jwt.getSubject());
        return buildTokenResponse(user);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token no reconocido"));
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);
    }

    private LoginResponse buildTokenResponse(AppUser user) {
        var now = Instant.now();
        var accessTokenExpiresAt = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);
        var refreshTokenExpiresAt = now.plus(refreshTokenExpirationDays, ChronoUnit.DAYS);
        var roles = List.of(user.getRole().name());

        var accessToken = generateToken(user.getUsername(), roles, "access", accessTokenExpiresAt, now);
        var refreshToken = generateToken(user.getUsername(), roles, "refresh", refreshTokenExpiresAt, now);
        persistRefreshToken(user, refreshToken, refreshTokenExpiresAt);

        return new LoginResponse("Bearer", accessToken, accessTokenExpiresAt, refreshToken, refreshTokenExpiresAt);
    }

    private String generateToken(String username, Object roles, String type, Instant expiresAt, Instant issuedAt) {
        var claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .issuer("edificio-app")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(username)
                .claim("type", type)
                .claim("roles", roles)
                .build();

        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    private Jwt decode(String token) {
        try {
            return refreshTokenJwtDecoder.decode(token);
        } catch (JwtException ex) {
            throw new InvalidTokenException("Refresh token invalido o expirado");
        }
    }

    private void validateRefreshJwt(Jwt token) {
        if (!"refresh".equals(token.getClaimAsString("type"))) {
            throw new InvalidTokenException("El token enviado no es un refresh token");
        }
    }

    private void validateStoredRefreshToken(RefreshToken token) {
        if (token.isRevoked()) {
            refreshTokenRepository.revokeAllActiveTokensByUserId(token.getUser().getId());
            throw new InvalidTokenException("Refresh token reutilizado. Todas las sesiones fueron revocadas.");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token revocado o expirado");
        }
    }

    private AppUser findActiveUser(String username) {
        var user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidTokenException("Usuario no encontrado"));
        if (!user.isActive()) {
            throw new InvalidTokenException("Usuario inactivo");
        }
        return user;
    }

    private void persistRefreshToken(AppUser user, String token, Instant expiresAt) {
        var refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
    }
}

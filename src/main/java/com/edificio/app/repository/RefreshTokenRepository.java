package com.edificio.app.repository;

import com.edificio.app.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken token set token.revoked = true where token.user.id = :userId and token.revoked = false")
    int revokeAllActiveTokensByUserId(UUID userId);
}

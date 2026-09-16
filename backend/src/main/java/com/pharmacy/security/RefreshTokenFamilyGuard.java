package com.pharmacy.security;

import com.pharmacy.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Commits family-wide refresh-token revocation in an independent transaction so a
 * subsequent BusinessException in the caller cannot roll back the revoke.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenFamilyGuard {
    private final RefreshTokenMapper refreshTokenMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void revokeFamilyCommitted(String familyId) {
        refreshTokenMapper.revokeFamily(familyId, LocalDateTime.now());
    }
}

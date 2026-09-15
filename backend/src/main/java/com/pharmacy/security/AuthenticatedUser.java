package com.pharmacy.security;

import com.pharmacy.enums.UserRole;

import java.io.Serializable;

public record AuthenticatedUser(Long id, String username, String nickname, UserRole role) implements Serializable {
}

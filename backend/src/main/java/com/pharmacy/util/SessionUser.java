
package com.pharmacy.util;

import com.pharmacy.enums.UserRole;
import java.io.Serializable;

public record SessionUser(Long id, String username, String nickname, UserRole role) implements Serializable { }

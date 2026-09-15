package com.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("refresh_token")
public class RefreshToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String tokenHash;
    private String familyId;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String replacedByHash;
    private String userAgent;
    private String ipAddress;
    private LocalDateTime createTime;
}

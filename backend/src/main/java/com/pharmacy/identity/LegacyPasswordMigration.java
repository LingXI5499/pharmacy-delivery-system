package com.pharmacy.identity;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.pharmacy.entity.SysUser;
import com.pharmacy.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time compatibility migration for installations created by the old SQL file.
 * Any encoding failure aborts application startup; plaintext is never accepted on login.
 */
@Component @RequiredArgsConstructor @Order(Ordered.HIGHEST_PRECEDENCE)
public class LegacyPasswordMigration implements ApplicationRunner {
    private final SysUserMapper users; private final PasswordEncoder encoder;
    @Override @Transactional(rollbackFor=Exception.class)
    public void run(ApplicationArguments args){
        for(SysUser user:users.selectList(Wrappers.<SysUser>lambdaQuery())){
            String password=user.getPassword();
            if(password!=null&&!password.startsWith("$2a$")&&!password.startsWith("$2b$")&&!password.startsWith("$2y$")){
                user.setPassword(encoder.encode(password));users.updateById(user);
            }
        }
    }
}

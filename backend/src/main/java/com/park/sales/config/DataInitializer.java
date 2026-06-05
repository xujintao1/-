package com.park.sales.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.entity.User;
import com.park.sales.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 首次启动时初始化默认账号（密码使用 BCrypt 编码）。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seed("admin", "admin123", "超级管理员", "ADMIN");
        seed("sales", "sales123", "销售小王", "SALES");
        seed("manager", "manager123", "销售经理老李", "SALES_MANAGER");
        seed("finance", "finance123", "财务主管", "FINANCE");
        seed("legal", "legal123", "法务专员", "LEGAL");
    }

    private void seed(String username, String rawPassword, String realName, String roles) {
        List<User> existing = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (!existing.isEmpty()) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(realName);
        user.setRoles(roles);
        user.setEnabled(1);
        userMapper.insert(user);
        log.info("初始化默认账号: {} / {}", username, rawPassword);
    }
}

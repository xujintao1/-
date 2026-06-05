package com.park.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.common.BusinessException;
import com.park.sales.entity.User;
import com.park.sales.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统用户管理（CRUD / 启停 / 重置密码）。
 */
@Service
public class SystemUserService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public SystemUserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> list(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword));
        }
        List<User> users = userMapper.selectList(wrapper);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public User get(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    public void create(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已存在");
        }
        String raw = (user.getPassword() == null || user.getPassword().isBlank())
                ? DEFAULT_PASSWORD : user.getPassword();
        user.setPassword(passwordEncoder.encode(raw));
        if (user.getEnabled() == null) {
            user.setEnabled(1);
        }
        userMapper.insert(user);
    }

    public void update(Long id, User user) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        existing.setRealName(user.getRealName());
        existing.setPhone(user.getPhone());
        existing.setRoles(user.getRoles());
        if (user.getEnabled() != null) {
            existing.setEnabled(user.getEnabled());
        }
        // 仅在传入新密码时更新
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.updateById(existing);
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    public void setEnabled(Long id, boolean enabled) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setEnabled(enabled ? 1 : 0);
        userMapper.updateById(user);
    }

    public String resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String raw = (newPassword == null || newPassword.isBlank()) ? DEFAULT_PASSWORD : newPassword;
        user.setPassword(passwordEncoder.encode(raw));
        userMapper.updateById(user);
        return raw;
    }
}

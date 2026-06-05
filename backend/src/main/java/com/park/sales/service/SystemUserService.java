package com.park.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.common.BusinessException;
import com.park.sales.entity.User;
import com.park.sales.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统管理 - 用户管理。
 *
 * <p>用户身份数据同步自 OA 系统（对齐 hr- 仓库 UserController/UserService）：列表以 OA 用户为准，
 * 本系统 sys_user 表只负责"授权"叠加——决定 OA 用户能否登录本系统(enabled)、拥有哪些角色(roles)。
 * OA 未配置或调用失败时回退本地 sys_user 数据，保证本地环境仍可用。
 */
@Service
public class SystemUserService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OaApiClient oaApiClient;

    public SystemUserService(UserMapper userMapper, PasswordEncoder passwordEncoder, OaApiClient oaApiClient) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.oaApiClient = oaApiClient;
    }

    /**
     * 用于"用户管理"页面的列表：OA 用户叠加本地授权（角色/启用状态）。
     * OA 未配置或返回空时，回退本地 sys_user。
     */
    public List<Map<String, Object>> listForManagement(String keyword, Long deptId) {
        if (oaApiClient.isConfigured()) {
            List<Map<String, Object>> oaUsers = (deptId != null)
                    ? oaApiClient.getOaUsersByDept(deptId)
                    : oaApiClient.getOaUsers(true);
            if (oaUsers != null && !oaUsers.isEmpty()) {
                return overlayLocalAuthorization(oaUsers, keyword);
            }
        }
        return localUsersAsManagement(keyword);
    }

    /** 把本地授权（roles/enabled）叠加到 OA 用户上 */
    private List<Map<String, Object>> overlayLocalAuthorization(List<Map<String, Object>> oaUsers, String keyword) {
        Map<String, User> localByUsername = new HashMap<>();
        for (User u : userMapper.selectList(null)) {
            if (u.getUsername() != null) {
                localByUsername.put(u.getUsername(), u);
            }
        }
        String kw = keyword == null ? null : keyword.trim().toLowerCase();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> oa : oaUsers) {
            String username = asString(oa.get("username"));
            String realName = asString(oa.get("realName"));
            if (kw != null && !kw.isEmpty()) {
                boolean match = (username != null && username.toLowerCase().contains(kw))
                        || (realName != null && realName.toLowerCase().contains(kw));
                if (!match) {
                    continue;
                }
            }
            User local = username == null ? null : localByUsername.get(username);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("oaUserId", oa.get("id"));
            row.put("id", local != null ? local.getId() : null);
            row.put("username", username);
            row.put("realName", realName);
            row.put("deptName", oa.get("deptName"));
            row.put("phone", oa.get("phone"));
            row.put("roles", local != null ? local.getRoles() : "");
            // enabled 取本地授权；无本地记录视为未授权(0)
            row.put("enabled", local != null && local.getEnabled() != null ? local.getEnabled() : 0);
            row.put("oaSourced", true);
            result.add(row);
        }
        return result;
    }

    /** OA 不可用时的本地回退列表 */
    private List<Map<String, Object>> localUsersAsManagement(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().orderByDesc(User::getId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : userMapper.selectList(wrapper)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("oaUserId", null);
            row.put("id", u.getId());
            row.put("username", u.getUsername());
            row.put("realName", u.getRealName());
            row.put("deptName", null);
            row.put("phone", u.getPhone());
            row.put("roles", u.getRoles());
            row.put("enabled", u.getEnabled());
            row.put("oaSourced", false);
            result.add(row);
        }
        return result;
    }

    /**
     * 分配授权：保存角色 + 启用状态（按 username upsert 本地 sys_user）。
     * 对齐 hr- 仓库 UserController.assignRole。
     */
    public void assignRole(String username, String realName, String phone, String deptName,
                           String roles, Integer enabled) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        User local = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (local == null) {
            local = new User();
            local.setUsername(username);
            local.setRealName(realName);
            local.setPhone(phone);
            local.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            local.setRoles(roles);
            local.setEnabled(enabled != null ? enabled : 1);
            userMapper.insert(local);
        } else {
            local.setRoles(roles);
            if (realName != null) local.setRealName(realName);
            if (phone != null) local.setPhone(phone);
            if (enabled != null) local.setEnabled(enabled);
            userMapper.updateById(local);
        }
    }

    public User get(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}

package com.park.sales.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.sales.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部 OA 系统 API 客户端，参照 hr- 仓库 OaApiClient 实现。
 *
 * <p>对接 OA 开放接口：发起/撤销/重新提交/删除流程、预览审批流、查询审批进度。
 * OA 地址优先从系统配置(sys_config: oa_api_url)读取，未配置时回落到 application.yml 默认值。
 * 当 OA 地址为空或调用异常时，方法返回 null/false，不影响本地审批流程（与 HR 行为一致）。
 */
@Service
public class OaApiClient {

    private static final Logger log = LoggerFactory.getLogger(OaApiClient.class);

    private final RestTemplate restTemplate;
    private final ConfigService configService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.oa.api-base-url:}")
    private String defaultOaApiUrl;

    public OaApiClient(ConfigService configService) {
        this.configService = configService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(30_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /** 取得 OA API 根地址：系统配置优先，其次 application.yml 默认值 */
    public String getOaApiUrl() {
        String url = configService.getConfig("oa_api_url");
        if (url == null || url.isBlank()) {
            url = defaultOaApiUrl;
        }
        return url == null ? "" : url.trim();
    }

    /** OA 是否可用（配置了地址） */
    public boolean isConfigured() {
        return !getOaApiUrl().isEmpty();
    }

    /**
     * 通过 OA 单点登录校验账号密码，调用 OA 的 /open-api/sso/login（form 表单）。
     * 参照 hr- 仓库 SsoService.ssoLogin 的 OA 调用部分。
     *
     * @return OA 返回的 data 节点（含 username/token/realName/avatar）
     * @throws BusinessException OA 校验失败或服务不可用时抛出，message 为 OA 返回的提示
     */
    public JsonNode ssoLogin(String username, String password) {
        String base = getOaApiUrl();
        if (base.isEmpty()) {
            throw new BusinessException(500, "未配置 OA 地址，无法进行单点登录");
        }
        String url = base + "/open-api/sso/login";
        log.info("[OaApiClient.ssoLogin] >>> POST {} username={}", url, username);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("username", username);
            params.add("password", password);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("code") && root.get("code").asInt() == 200) {
                return root.get("data");
            }
            String msg = root.has("message") ? root.get("message").asText() : "OA 登录失败";
            throw new BusinessException(400, msg);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[OaApiClient.ssoLogin] OA 单点登录失败: {} ({})", e.getMessage(), e.getClass().getSimpleName());
            throw new BusinessException(500, "OA 单点登录服务不可用: " + e.getMessage());
        }
    }

    /**
     * 校验 OA Token 有效性，调用 OA 的 /open-api/sso/validate（Bearer）。
     * 参照 hr- 仓库 SsoService.validateToken。
     *
     * @return OA 返回的 data 节点（含 valid/username/realName/avatar），异常时返回 null
     */
    public JsonNode validateToken(String token) {
        String base = getOaApiUrl();
        if (base.isEmpty() || token == null || token.isEmpty()) {
            return null;
        }
        String url = base + "/open-api/sso/validate";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("code") && root.get("code").asInt() == 200) {
                return root.get("data");
            }
            return null;
        } catch (Exception e) {
            log.error("[OaApiClient.validateToken] OA Token 校验异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 向 OA 发起审批流程。
     *
     * @return OA 返回的 data（含 oaProcessInstanceId 等），失败返回 null。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> startProcess(Long externalInstanceId,
                                            String workflowType,
                                            String title,
                                            Map<String, Object> formData,
                                            String callbackUrl) {
        String base = getOaApiUrl();
        if (base.isEmpty()) {
            log.warn("[OaApiClient] 未配置 OA 地址，跳过 OA 发起: externalInstanceId={}", externalInstanceId);
            return null;
        }
        String url = base + "/open-api/process/start";

        Map<String, Object> request = new HashMap<>();
        request.put("externalInstanceId", externalInstanceId);
        request.put("workflowType", workflowType);
        request.put("title", title);
        request.put("formData", formData);
        request.put("callbackUrl", callbackUrl);
        request.put("sourceSystem", "PARK_SALES");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.info("[OaApiClient.startProcess] >>> POST {} externalInstanceId={}", url, externalInstanceId);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object code = response.getBody().get("code");
                if (code != null && Integer.parseInt(code.toString()) == 200) {
                    log.info("[OaApiClient.startProcess] OA 流程创建成功: externalInstanceId={}", externalInstanceId);
                    return (Map<String, Object>) response.getBody().get("data");
                }
            }
            log.error("[OaApiClient.startProcess] OA 流程创建失败: {}", response.getBody());
            return null;
        } catch (Exception e) {
            log.error("[OaApiClient.startProcess] 调用 OA 失败: {} ({})", e.getMessage(), e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * 撤销 OA 流程。调用 OA 的 /open-api/process/cancel/{id}。
     */
    public boolean cancelProcess(String oaProcessInstanceId, String reason) {
        if (oaProcessInstanceId == null || oaProcessInstanceId.isEmpty() || getOaApiUrl().isEmpty()) {
            log.warn("[OaApiClient.cancelProcess] 跳过 OA 撤销: id={}", oaProcessInstanceId);
            return false;
        }
        String url = getOaApiUrl() + "/open-api/process/cancel/" + oaProcessInstanceId;
        Map<String, Object> request = new HashMap<>();
        request.put("reason", reason != null ? reason : "销售系统撤销");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return is200(response);
        } catch (Exception e) {
            log.error("[OaApiClient.cancelProcess] 调用 OA 撤销异常: id={}, err={}", oaProcessInstanceId, e.getMessage());
            return false;
        }
    }

    /**
     * 重新提交 OA 流程（撤销/退回后）。调用 OA 的 /open-api/process/resubmit/{id}。
     */
    public boolean resubmitProcess(String oaProcessInstanceId, String formData, String comment) {
        if (oaProcessInstanceId == null || oaProcessInstanceId.isEmpty() || getOaApiUrl().isEmpty()) {
            log.warn("[OaApiClient.resubmitProcess] 跳过 OA 重新提交: id={}", oaProcessInstanceId);
            return false;
        }
        String url = getOaApiUrl() + "/open-api/process/resubmit/" + oaProcessInstanceId;
        Map<String, Object> request = new HashMap<>();
        if (formData != null) request.put("formData", formData);
        if (comment != null) request.put("comment", comment);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return is200(response);
        } catch (Exception e) {
            log.error("[OaApiClient.resubmitProcess] 调用 OA 重新提交异常: id={}, err={}", oaProcessInstanceId, e.getMessage());
            return false;
        }
    }

    /**
     * 删除 OA 流程实例。调用 OA 的 DELETE /open-api/process/instance/{id}。
     */
    public boolean deleteProcess(String oaProcessInstanceId) {
        if (oaProcessInstanceId == null || oaProcessInstanceId.isEmpty() || getOaApiUrl().isEmpty()) {
            log.warn("[OaApiClient.deleteProcess] 跳过 OA 删除: id={}", oaProcessInstanceId);
            return false;
        }
        String url = getOaApiUrl() + "/open-api/process/instance/" + oaProcessInstanceId;
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.DELETE, HttpEntity.EMPTY, Map.class);
            return is200(response);
        } catch (Exception e) {
            log.error("[OaApiClient.deleteProcess] 调用 OA 删除异常: id={}, err={}", oaProcessInstanceId, e.getMessage());
            return false;
        }
    }

    /**
     * 预览 OA 审批流程定义。调用 OA 的 /open-api/process/preview/{processKey}。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> previewApprovalFlow(String processKey) {
        if (getOaApiUrl().isEmpty()) {
            return null;
        }
        String url = getOaApiUrl() + "/open-api/process/preview/" + processKey;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (is200(response)) {
                return (Map<String, Object>) response.getBody().get("data");
            }
            return null;
        } catch (Exception e) {
            log.error("[OaApiClient.previewApprovalFlow] 预览失败: key={}, err={}", processKey, e.getMessage());
            return null;
        }
    }

    /**
     * 查询 OA 流程审批进度。调用 OA 的 /open-api/process/progress/{instanceId}。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getApprovalProgress(String oaProcessInstanceId) {
        if (oaProcessInstanceId == null || oaProcessInstanceId.isEmpty() || getOaApiUrl().isEmpty()) {
            return null;
        }
        String url = getOaApiUrl() + "/open-api/process/progress/" + oaProcessInstanceId + "?sourceSystem=PARK_SALES";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (is200(response)) {
                return (Map<String, Object>) response.getBody().get("data");
            }
            return null;
        } catch (Exception e) {
            log.error("[OaApiClient.getApprovalProgress] 查询进度失败: id={}, err={}", oaProcessInstanceId, e.getMessage());
            return null;
        }
    }

    /**
     * 获取 OA 部门树。调用 OA 的 /open-api/depts/tree。
     * 参照 hr- 仓库 OaApiClient.getOaDeptTree。OA 未配置或异常时返回空列表。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getOaDeptTree() {
        if (getOaApiUrl().isEmpty()) {
            return Collections.emptyList();
        }
        String url = getOaApiUrl() + "/open-api/depts/tree";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (is200(response)) {
                Object data = response.getBody().get("data");
                if (data instanceof List) {
                    return (List<Map<String, Object>>) data;
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[OaApiClient.getOaDeptTree] 获取 OA 部门树失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 获取 OA 所有用户（默认只含已启用用户）。 */
    public List<Map<String, Object>> getOaUsers() {
        return getOaUsers(false);
    }

    /**
     * 获取 OA 所有用户。调用 OA 的 /open-api/users/all。
     * 参照 hr- 仓库 OaApiClient.getOaUsers，兼容 PageResult(data.records) 与 List 两种返回格式。
     *
     * @param includeDisabled 是否包含已禁用/离职用户
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getOaUsers(boolean includeDisabled) {
        if (getOaApiUrl().isEmpty()) {
            return Collections.emptyList();
        }
        String url = getOaApiUrl() + "/open-api/users/all"
                + (includeDisabled ? "?includeDisabled=true" : "");
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (is200(response)) {
                Object data = response.getBody().get("data");
                if (data instanceof Map) {
                    Object records = ((Map<String, Object>) data).get("records");
                    if (records instanceof List) {
                        return (List<Map<String, Object>>) records;
                    }
                }
                if (data instanceof List) {
                    return (List<Map<String, Object>>) data;
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[OaApiClient.getOaUsers] 获取 OA 用户列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取 OA 指定部门下的用户。调用 OA 的 /open-api/users/by-dept/{deptId}。
     * 参照 hr- 仓库 OaApiClient.getOaUsersByDept。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getOaUsersByDept(Long deptId) {
        if (deptId == null || getOaApiUrl().isEmpty()) {
            return Collections.emptyList();
        }
        String url = getOaApiUrl() + "/open-api/users/by-dept/" + deptId;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (is200(response)) {
                Object data = response.getBody().get("data");
                if (data instanceof List) {
                    return (List<Map<String, Object>>) data;
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[OaApiClient.getOaUsersByDept] 获取 OA 部门用户失败: deptId={}, err={}", deptId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean is200(ResponseEntity<Map> response) {
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object code = response.getBody().get("code");
            return code != null && Integer.parseInt(code.toString()) == 200;
        }
        return false;
    }
}

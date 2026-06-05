package com.park.sales.controller;

import com.park.sales.common.Result;
import com.park.sales.service.ApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 外部 OA 审批回调入口。
 *
 * <p>OA 系统在审批状态变更或某节点审批完成后，回调本系统对应接口同步结果。
 * 该路径在 {@code SecurityConfig} 中已放行（permitAll），无需登录态。
 */
@RestController
@RequestMapping("/oa/callback")
public class OaCallbackController {

    private static final Logger log = LoggerFactory.getLogger(OaCallbackController.class);

    private final ApprovalService approvalService;

    public OaCallbackController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    /**
     * 审批流整体状态回调。
     * 期望字段：externalNo（或 oaInstanceId / instanceId）、status（或 oaStatus）、message。
     */
    @PostMapping("/status")
    public Result<Void> status(@RequestBody Map<String, Object> body) {
        log.info("[OA回调-状态] body={}", body);
        String externalNo = str(body, "externalNo", "oaInstanceId", "instanceId", "processInstanceId");
        String status = str(body, "status", "oaStatus", "result");
        String message = str(body, "message", "comment");
        approvalService.handleStatusCallback(externalNo, status, message);
        return Result.ok();
    }

    /**
     * 单节点审批记录回调。
     * 期望字段：externalNo、step、approverName、result、comment。
     */
    @PostMapping("/record")
    public Result<Void> record(@RequestBody Map<String, Object> body) {
        log.info("[OA回调-记录] body={}", body);
        String externalNo = str(body, "externalNo", "oaInstanceId", "instanceId", "processInstanceId");
        Integer step = intVal(body, "step");
        String approverName = str(body, "approverName", "approver");
        String result = str(body, "result", "status");
        String comment = str(body, "comment", "message");
        approvalService.handleRecordCallback(externalNo, step, approverName, result, comment);
        return Result.ok();
    }

    private String str(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object v = body.get(key);
            if (v != null) {
                return v.toString();
            }
        }
        return null;
    }

    private Integer intVal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) {
            return null;
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

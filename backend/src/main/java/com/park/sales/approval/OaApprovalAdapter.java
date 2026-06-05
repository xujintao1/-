package com.park.sales.approval;

import com.park.sales.entity.ApprovalFlow;
import com.park.sales.entity.ApprovalTask;
import com.park.sales.mapper.ApprovalFlowMapper;
import com.park.sales.mapper.ApprovalTaskMapper;
import com.park.sales.service.ConfigService;
import com.park.sales.service.OaApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部 OA 审批适配器。
 *
 * <p>发起时调用 OA 开放接口（{@link OaApiClient#startProcess}）创建审批实例，
 * 并把 OA 返回的实例号保存到 {@code externalNo}；同时在本地落审批流/任务，
 * 以便在 OA 未配置或回调未到达时仍可在系统内跟踪与展示。
 *
 * <p>OA 审批结果通过回调接口（{@code /api/oa/callback/status}）同步回本系统。
 * 撤销 / 重新提交 / 删除会转调 OA 的对应开放接口；当 OA 地址未配置时，
 * 仅维护本地状态，行为与内置审批流一致（便于无 OA 环境下测试）。
 */
@Component
public class OaApprovalAdapter implements ApprovalGateway {

    private static final Logger log = LoggerFactory.getLogger(OaApprovalAdapter.class);

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalTaskMapper taskMapper;
    private final OaApiClient oaApiClient;
    private final ConfigService configService;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    public OaApprovalAdapter(ApprovalFlowMapper flowMapper,
                             ApprovalTaskMapper taskMapper,
                             OaApiClient oaApiClient,
                             ConfigService configService) {
        this.flowMapper = flowMapper;
        this.taskMapper = taskMapper;
        this.oaApiClient = oaApiClient;
        this.configService = configService;
    }

    @Override
    public String getType() {
        return "oa";
    }

    @Override
    public ApprovalFlow start(String bizType, Long bizId, List<ApprovalNodeDef> chain) {
        // 1) 先在本地创建审批流与节点任务
        ApprovalFlow flow = new ApprovalFlow();
        flow.setBizType(bizType);
        flow.setBizId(bizId);
        flow.setGateway(getType());
        flow.setCurrentStep(1);
        flow.setStatus("APPROVING");
        flowMapper.insert(flow);

        int step = 1;
        for (ApprovalNodeDef node : chain) {
            ApprovalTask task = new ApprovalTask();
            task.setFlowId(flow.getId());
            task.setStep(step++);
            task.setNodeName(node.nodeName());
            task.setApproverRole(node.role());
            task.setStatus("PENDING");
            taskMapper.insert(task);
        }

        // 2) 推送到外部 OA（未配置 OA 时跳过，仅保留本地流程）
        String workflowType = configService.getConfig(bizType.toLowerCase() + "_workflow_type",
                bizType + "_APPROVAL");
        Map<String, Object> formData = new HashMap<>();
        formData.put("bizType", bizType);
        formData.put("bizId", bizId);
        String title = bizType + " 审批 #" + bizId;

        Map<String, Object> data = oaApiClient.startProcess(flow.getId(), workflowType, title, formData, buildCallbackUrl());
        if (data != null) {
            String instanceId = firstNonNull(data, "oaProcessInstanceId", "processInstanceId", "instanceId");
            if (instanceId != null) {
                flow.setExternalNo(instanceId);
                flowMapper.updateById(flow);
                log.info("[OA] 审批已推送到外部 OA: bizType={}, bizId={}, externalNo={}", bizType, bizId, instanceId);
            }
        } else {
            // OA 未配置或调用失败：标记一个本地外部号，方便排查
            flow.setExternalNo("LOCAL-" + flow.getId());
            flowMapper.updateById(flow);
            log.info("[OA] 未配置/未连通外部 OA，按本地审批流处理: bizType={}, bizId={}", bizType, bizId);
        }
        return flow;
    }

    @Override
    public void cancel(ApprovalFlow flow, String reason) {
        if (hasExternal(flow)) {
            boolean ok = oaApiClient.cancelProcess(flow.getExternalNo(), reason);
            log.info("[OA] 撤销外部流程 externalNo={}, 结果={}", flow.getExternalNo(), ok);
        }
    }

    @Override
    public void resubmit(ApprovalFlow flow) {
        if (hasExternal(flow)) {
            boolean ok = oaApiClient.resubmitProcess(flow.getExternalNo(), null, "销售系统重新提交");
            log.info("[OA] 重新提交外部流程 externalNo={}, 结果={}", flow.getExternalNo(), ok);
        }
    }

    @Override
    public void delete(ApprovalFlow flow) {
        if (hasExternal(flow)) {
            boolean ok = oaApiClient.deleteProcess(flow.getExternalNo());
            log.info("[OA] 删除外部流程 externalNo={}, 结果={}", flow.getExternalNo(), ok);
        }
    }

    private boolean hasExternal(ApprovalFlow flow) {
        return flow.getExternalNo() != null
                && !flow.getExternalNo().isBlank()
                && !flow.getExternalNo().startsWith("LOCAL-");
    }

    private String buildCallbackUrl() {
        String configured = configService.getConfig("oa_callback_url");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String ctx = contextPath == null ? "" : contextPath;
        return String.format("http://localhost:%d%s/oa/callback/status", serverPort, ctx);
    }

    private String firstNonNull(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object v = data.get(key);
            if (v != null) {
                return v.toString();
            }
        }
        return null;
    }
}

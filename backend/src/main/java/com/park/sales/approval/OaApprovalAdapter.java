package com.park.sales.approval;

import com.park.sales.entity.ApprovalFlow;
import com.park.sales.entity.ApprovalTask;
import com.park.sales.mapper.ApprovalFlowMapper;
import com.park.sales.mapper.ApprovalTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 外部 OA 审批适配器（预留）。
 * <p>
 * 实际接入时，{@link #start} 应调用企业 OA 的开放接口（钉钉 / 企业微信 / 泛微 / 致远 等）
 * 创建审批实例，并保存返回的实例编号到 {@code externalNo}；
 * OA 审批结果通过回调接口（见 OaCallbackController，可后续补充）同步回本系统。
 * <p>
 * 当前为占位实现：生成模拟实例号并落库审批流，便于在不接入真实 OA 时演示切换能力。
 */
@Component
public class OaApprovalAdapter implements ApprovalGateway {

    private static final Logger log = LoggerFactory.getLogger(OaApprovalAdapter.class);

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalTaskMapper taskMapper;

    public OaApprovalAdapter(ApprovalFlowMapper flowMapper, ApprovalTaskMapper taskMapper) {
        this.flowMapper = flowMapper;
        this.taskMapper = taskMapper;
    }

    @Override
    public String getType() {
        return "oa";
    }

    @Override
    public ApprovalFlow start(String bizType, Long bizId, List<ApprovalNodeDef> chain) {
        String externalNo = "OA-" + System.currentTimeMillis();
        // TODO: 调用真实 OA 开放接口创建审批实例，externalNo 用 OA 返回的实例编号替换
        log.info("[OA] 推送审批至外部OA, bizType={}, bizId={}, externalNo={}", bizType, bizId, externalNo);

        ApprovalFlow flow = new ApprovalFlow();
        flow.setBizType(bizType);
        flow.setBizId(bizId);
        flow.setGateway(getType());
        flow.setExternalNo(externalNo);
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
        return flow;
    }
}

package com.park.sales.approval;

import com.park.sales.entity.ApprovalFlow;
import com.park.sales.entity.ApprovalTask;
import com.park.sales.mapper.ApprovalFlowMapper;
import com.park.sales.mapper.ApprovalTaskMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内置审批流适配器：在本系统内创建审批流与节点任务，由系统驱动多级审批。
 */
@Component
public class InternalApprovalAdapter implements ApprovalGateway {

    private final ApprovalFlowMapper flowMapper;
    private final ApprovalTaskMapper taskMapper;

    public InternalApprovalAdapter(ApprovalFlowMapper flowMapper, ApprovalTaskMapper taskMapper) {
        this.flowMapper = flowMapper;
        this.taskMapper = taskMapper;
    }

    @Override
    public String getType() {
        return "internal";
    }

    @Override
    public ApprovalFlow start(String bizType, Long bizId, List<ApprovalNodeDef> chain) {
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
        return flow;
    }
}

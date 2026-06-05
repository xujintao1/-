package com.park.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.common.BusinessException;
import com.park.sales.entity.ApprovalFlow;
import com.park.sales.entity.ApprovalTask;
import com.park.sales.entity.Contract;
import com.park.sales.entity.FactoryUnit;
import com.park.sales.mapper.ApprovalFlowMapper;
import com.park.sales.mapper.ApprovalTaskMapper;
import com.park.sales.mapper.ContractMapper;
import com.park.sales.mapper.FactoryUnitMapper;
import com.park.sales.security.LoginUser;
import com.park.sales.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApprovalService {

    private final ApprovalTaskMapper taskMapper;
    private final ApprovalFlowMapper flowMapper;
    private final ContractMapper contractMapper;
    private final FactoryUnitMapper factoryUnitMapper;

    public ApprovalService(ApprovalTaskMapper taskMapper,
                           ApprovalFlowMapper flowMapper,
                           ContractMapper contractMapper,
                           FactoryUnitMapper factoryUnitMapper) {
        this.taskMapper = taskMapper;
        this.flowMapper = flowMapper;
        this.contractMapper = contractMapper;
        this.factoryUnitMapper = factoryUnitMapper;
    }

    /** 当前登录用户的审批待办 */
    public List<ApprovalTask> todo() {
        LoginUser user = SecurityUtil.currentUser();
        List<String> roles = user.getRoleCodes();
        if (roles.isEmpty()) {
            return List.of();
        }
        // ADMIN 可见全部待办
        if (roles.contains("ADMIN")) {
            return taskMapper.selectTodo(null);
        }
        return taskMapper.selectTodo(roles);
    }

    public List<ApprovalTask> tasksOfFlow(Long flowId) {
        return taskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getFlowId, flowId)
                .orderByAsc(ApprovalTask::getStep));
    }

    @Transactional
    public void approve(Long taskId, String comment) {
        ApprovalTask task = loadActiveTask(taskId);
        ApprovalFlow flow = flowMapper.selectById(task.getFlowId());

        task.setStatus("APPROVED");
        task.setComment(comment);
        task.setApproverId(SecurityUtil.currentUserId());
        task.setHandledTime(LocalDateTime.now());
        taskMapper.updateById(task);

        long totalSteps = taskMapper.selectCount(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getFlowId, flow.getId()));
        if (task.getStep() < totalSteps) {
            flow.setCurrentStep(task.getStep() + 1);
            flowMapper.updateById(flow);
        } else {
            flow.setStatus("APPROVED");
            flowMapper.updateById(flow);
            onFlowApproved(flow);
        }
    }

    @Transactional
    public void reject(Long taskId, String comment) {
        ApprovalTask task = loadActiveTask(taskId);
        ApprovalFlow flow = flowMapper.selectById(task.getFlowId());

        task.setStatus("REJECTED");
        task.setComment(comment);
        task.setApproverId(SecurityUtil.currentUserId());
        task.setHandledTime(LocalDateTime.now());
        taskMapper.updateById(task);

        flow.setStatus("REJECTED");
        flowMapper.updateById(flow);

        if ("CONTRACT".equals(flow.getBizType())) {
            Contract contract = contractMapper.selectById(flow.getBizId());
            if (contract != null) {
                contract.setStatus("REJECTED");
                contractMapper.updateById(contract);
            }
        }
    }

    private void onFlowApproved(ApprovalFlow flow) {
        if (!"CONTRACT".equals(flow.getBizType())) {
            return;
        }
        Contract contract = contractMapper.selectById(flow.getBizId());
        if (contract == null) {
            return;
        }
        contract.setStatus("EFFECTIVE");
        contractMapper.updateById(contract);

        FactoryUnit unit = factoryUnitMapper.selectById(contract.getFactoryUnitId());
        if (unit != null) {
            unit.setStatus("SIGNED");
            factoryUnitMapper.updateById(unit);
        }
    }

    private ApprovalTask loadActiveTask(Long taskId) {
        ApprovalTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("审批任务不存在");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("该审批任务已处理");
        }
        ApprovalFlow flow = flowMapper.selectById(task.getFlowId());
        if (flow == null || !"APPROVING".equals(flow.getStatus())) {
            throw new BusinessException("审批流不存在或已结束");
        }
        if (!task.getStep().equals(flow.getCurrentStep())) {
            throw new BusinessException("尚未轮到该节点审批");
        }
        LoginUser user = SecurityUtil.currentUser();
        List<String> roles = user.getRoleCodes();
        if (!roles.contains("ADMIN") && !roles.contains(task.getApproverRole())) {
            throw new BusinessException(403, "您没有该节点的审批权限（需角色：" + task.getApproverRole() + "）");
        }
        return task;
    }
}

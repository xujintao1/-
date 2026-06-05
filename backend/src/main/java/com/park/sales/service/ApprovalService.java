package com.park.sales.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.approval.ApprovalGatewayFactory;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private static final Map<String, String> NODE_NAMES = new LinkedHashMap<>();

    static {
        NODE_NAMES.put("SALES_MANAGER", "销售经理审批");
        NODE_NAMES.put("FINANCE", "财务审批");
        NODE_NAMES.put("LEGAL", "法务审批");
        NODE_NAMES.put("GM", "总经理审批");
    }

    private final ApprovalTaskMapper taskMapper;
    private final ApprovalFlowMapper flowMapper;
    private final ContractMapper contractMapper;
    private final FactoryUnitMapper factoryUnitMapper;
    private final ApprovalGatewayFactory gatewayFactory;

    @Value("${app.approval.chain:SALES_MANAGER,FINANCE,LEGAL}")
    private String approvalChain;

    public ApprovalService(ApprovalTaskMapper taskMapper,
                           ApprovalFlowMapper flowMapper,
                           ContractMapper contractMapper,
                           FactoryUnitMapper factoryUnitMapper,
                           ApprovalGatewayFactory gatewayFactory) {
        this.taskMapper = taskMapper;
        this.flowMapper = flowMapper;
        this.contractMapper = contractMapper;
        this.factoryUnitMapper = factoryUnitMapper;
        this.gatewayFactory = gatewayFactory;
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

    /** 审批流详情：流程 + 节点任务（供前端时间线展示） */
    public Map<String, Object> flowDetail(Long flowId) {
        ApprovalFlow flow = requireFlow(flowId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flow", flow);
        result.put("tasks", tasksOfFlow(flowId));
        return result;
    }

    /**
     * 审批流程预览（按配置的审批链生成节点，供发起前预览）。
     */
    public Map<String, Object> preview(String bizType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processName", "CONTRACT".equalsIgnoreCase(bizType) ? "合同审批流程" : bizType + " 审批流程");
        List<Map<String, Object>> nodes = new ArrayList<>();
        // 发起人节点
        nodes.add(node("发起申请", 0, null));
        for (String role : approvalChain.split(",")) {
            String r = role.trim();
            if (r.isEmpty()) {
                continue;
            }
            nodes.add(node(NODE_NAMES.getOrDefault(r, r + " 审批"), 1, r));
        }
        result.put("nodes", nodes);
        return result;
    }

    private Map<String, Object> node(String nodeName, int type, String role) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("nodeName", nodeName);
        n.put("type", type);
        if (role != null) {
            List<Map<String, Object>> approvers = new ArrayList<>();
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("name", NODE_NAMES.getOrDefault(role, role).replace("审批", ""));
            a.put("role", role);
            approvers.add(a);
            n.put("approvers", approvers);
        }
        return n;
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

        setContractStatus(flow, "REJECTED");
    }

    /**
     * 撤销审批流（发起人/管理员主动撤回）。
     * 转调网关撤销外部 OA，并把本地流程置为 WITHDRAWN、合同回退到草稿。
     */
    @Transactional
    public void withdraw(Long flowId, String reason) {
        ApprovalFlow flow = requireFlow(flowId);
        if (!"APPROVING".equals(flow.getStatus())) {
            throw new BusinessException("仅审批中的流程可撤销");
        }
        checkInitiatorPermission(flow);

        gatewayFactory.byType(flow.getGateway()).cancel(flow, reason);

        flow.setStatus("WITHDRAWN");
        flowMapper.updateById(flow);
        // 作废尚未处理的待办任务
        for (ApprovalTask task : tasksOfFlow(flowId)) {
            if ("PENDING".equals(task.getStatus())) {
                task.setStatus("CANCELLED");
                task.setComment(reason);
                taskMapper.updateById(task);
            }
        }
        setContractStatus(flow, "DRAFT");
    }

    /**
     * 重新提交审批流（撤销或驳回后）。
     * 转调网关重新提交外部 OA，并把本地任务全部重置为待审、流程回到第一步。
     */
    @Transactional
    public void resubmit(Long flowId) {
        ApprovalFlow flow = requireFlow(flowId);
        if (!"WITHDRAWN".equals(flow.getStatus()) && !"REJECTED".equals(flow.getStatus())) {
            throw new BusinessException("仅已撤销或已驳回的流程可重新提交");
        }
        checkInitiatorPermission(flow);

        gatewayFactory.byType(flow.getGateway()).resubmit(flow);

        for (ApprovalTask task : tasksOfFlow(flowId)) {
            task.setStatus("PENDING");
            task.setComment(null);
            task.setApproverId(null);
            task.setHandledTime(null);
            taskMapper.updateById(task);
        }
        flow.setStatus("APPROVING");
        flow.setCurrentStep(1);
        flowMapper.updateById(flow);
        setContractStatus(flow, "APPROVING");
    }

    /**
     * 删除审批流（含外部 OA 实例），合同回退到草稿并解除关联。
     */
    @Transactional
    public void delete(Long flowId) {
        ApprovalFlow flow = requireFlow(flowId);
        if ("APPROVING".equals(flow.getStatus())) {
            throw new BusinessException("请先撤销审批流再删除");
        }
        checkInitiatorPermission(flow);

        gatewayFactory.byType(flow.getGateway()).delete(flow);

        for (ApprovalTask task : tasksOfFlow(flowId)) {
            taskMapper.deleteById(task.getId());
        }
        flowMapper.deleteById(flowId);

        if ("CONTRACT".equals(flow.getBizType())) {
            Contract contract = contractMapper.selectById(flow.getBizId());
            if (contract != null) {
                contract.setStatus("DRAFT");
                contract.setApprovalFlowId(null);
                contractMapper.updateById(contract);
            }
        }
    }

    /**
     * OA 回调：更新审批流状态。
     * 按 externalNo 找到本地流程，映射 OA 状态并驱动后续业务。
     */
    @Transactional
    public void handleStatusCallback(String externalNo, String oaStatus, String message) {
        if (externalNo == null || externalNo.isBlank()) {
            log.warn("[OA回调] externalNo 为空，忽略");
            return;
        }
        ApprovalFlow flow = flowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getExternalNo, externalNo));
        if (flow == null) {
            log.warn("[OA回调] 未找到流程 externalNo={}", externalNo);
            return;
        }
        String status = mapOaStatus(oaStatus);
        log.info("[OA回调] externalNo={}, oaStatus={} -> {}, msg={}", externalNo, oaStatus, status, message);
        switch (status) {
            case "APPROVED" -> {
                flow.setStatus("APPROVED");
                flow.setCurrentStep((int) taskCount(flow.getId()));
                flowMapper.updateById(flow);
                markAllTasks(flow.getId(), "APPROVED", message);
                onFlowApproved(flow);
            }
            case "REJECTED" -> {
                flow.setStatus("REJECTED");
                flowMapper.updateById(flow);
                setContractStatus(flow, "REJECTED");
            }
            default -> {
                flow.setStatus("APPROVING");
                flowMapper.updateById(flow);
            }
        }
    }

    /**
     * OA 回调：推送单条审批记录（某节点审批结果）。
     */
    @Transactional
    public void handleRecordCallback(String externalNo, Integer step, String approverName,
                                     String result, String comment) {
        ApprovalFlow flow = flowMapper.selectOne(new LambdaQueryWrapper<ApprovalFlow>()
                .eq(ApprovalFlow::getExternalNo, externalNo));
        if (flow == null) {
            log.warn("[OA回调-记录] 未找到流程 externalNo={}", externalNo);
            return;
        }
        List<ApprovalTask> tasks = tasksOfFlow(flow.getId());
        ApprovalTask target = tasks.stream()
                .filter(t -> step != null && step.equals(t.getStep()))
                .findFirst()
                .orElse(null);
        if (target == null) {
            log.warn("[OA回调-记录] 未匹配到节点 step={}, externalNo={}", step, externalNo);
            return;
        }
        target.setStatus("REJECT".equalsIgnoreCase(result) || "REJECTED".equalsIgnoreCase(result)
                ? "REJECTED" : "APPROVED");
        target.setComment(comment);
        target.setHandledTime(LocalDateTime.now());
        taskMapper.updateById(target);
        log.info("[OA回调-记录] 已更新节点 step={}, approver={}, result={}", step, approverName, result);
    }

    // ---------------- 内部辅助 ----------------

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

    private void setContractStatus(ApprovalFlow flow, String status) {
        if (!"CONTRACT".equals(flow.getBizType())) {
            return;
        }
        Contract contract = contractMapper.selectById(flow.getBizId());
        if (contract != null) {
            contract.setStatus(status);
            contractMapper.updateById(contract);
        }
    }

    private long taskCount(Long flowId) {
        return taskMapper.selectCount(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getFlowId, flowId));
    }

    private void markAllTasks(Long flowId, String status, String comment) {
        for (ApprovalTask task : tasksOfFlow(flowId)) {
            if ("PENDING".equals(task.getStatus())) {
                task.setStatus(status);
                task.setComment(comment);
                task.setHandledTime(LocalDateTime.now());
                taskMapper.updateById(task);
            }
        }
    }

    private String mapOaStatus(String oaStatus) {
        if (oaStatus == null) {
            return "APPROVING";
        }
        String s = oaStatus.trim().toUpperCase();
        return switch (s) {
            case "APPROVED", "COMPLETE", "COMPLETED", "PASS", "AGREE", "FINISH" -> "APPROVED";
            case "REJECTED", "REJECT", "REFUSE", "TERMINATED", "TERMINATE" -> "REJECTED";
            default -> "APPROVING";
        };
    }

    private ApprovalFlow requireFlow(Long flowId) {
        ApprovalFlow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            throw new BusinessException("审批流不存在");
        }
        return flow;
    }

    private void checkInitiatorPermission(ApprovalFlow flow) {
        LoginUser user = SecurityUtil.currentUser();
        if (user.getRoleCodes().contains("ADMIN")) {
            return;
        }
        if ("CONTRACT".equals(flow.getBizType())) {
            Contract contract = contractMapper.selectById(flow.getBizId());
            if (contract != null && user.getId().equals(contract.getSalesId())) {
                return;
            }
        }
        throw new BusinessException(403, "仅发起人或管理员可执行该操作");
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

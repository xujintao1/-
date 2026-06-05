package com.park.sales.controller;

import com.park.sales.common.Result;
import com.park.sales.dto.ApprovalActionRequest;
import com.park.sales.entity.ApprovalTask;
import com.park.sales.service.ApprovalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/todo")
    public Result<List<ApprovalTask>> todo() {
        return Result.ok(approvalService.todo());
    }

    /** 审批流程预览（发起前查看节点） */
    @GetMapping("/preview")
    public Result<Map<String, Object>> preview(@RequestParam(defaultValue = "CONTRACT") String bizType) {
        return Result.ok(approvalService.preview(bizType));
    }

    /** 审批流详情（流程 + 节点任务，用于时间线展示） */
    @GetMapping("/flows/{flowId}")
    public Result<Map<String, Object>> flowDetail(@PathVariable Long flowId) {
        return Result.ok(approvalService.flowDetail(flowId));
    }

    @PostMapping("/{taskId}/approve")
    public Result<Void> approve(@PathVariable Long taskId, @RequestBody(required = false) ApprovalActionRequest req) {
        approvalService.approve(taskId, req == null ? null : req.getComment());
        return Result.ok();
    }

    @PostMapping("/{taskId}/reject")
    public Result<Void> reject(@PathVariable Long taskId, @RequestBody(required = false) ApprovalActionRequest req) {
        approvalService.reject(taskId, req == null ? null : req.getComment());
        return Result.ok();
    }

    /** 撤销审批流（发起人/管理员） */
    @PostMapping("/flows/{flowId}/withdraw")
    public Result<Void> withdraw(@PathVariable Long flowId, @RequestBody(required = false) ApprovalActionRequest req) {
        approvalService.withdraw(flowId, req == null ? null : req.getComment());
        return Result.ok();
    }

    /** 重新提交审批流 */
    @PostMapping("/flows/{flowId}/resubmit")
    public Result<Void> resubmit(@PathVariable Long flowId) {
        approvalService.resubmit(flowId);
        return Result.ok();
    }

    /** 删除审批流 */
    @DeleteMapping("/flows/{flowId}")
    public Result<Void> delete(@PathVariable Long flowId) {
        approvalService.delete(flowId);
        return Result.ok();
    }

    /** 重新同步（状态回调的手动触发，以 OA 端状态为准校准本地流程，仅管理员） */
    @PostMapping("/flows/{flowId}/resync")
    public Result<String> resync(@PathVariable Long flowId) {
        return Result.ok(approvalService.resync(flowId));
    }
}

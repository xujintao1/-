package com.park.sales.controller;

import com.park.sales.common.Result;
import com.park.sales.dto.ApprovalActionRequest;
import com.park.sales.entity.ApprovalTask;
import com.park.sales.service.ApprovalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}

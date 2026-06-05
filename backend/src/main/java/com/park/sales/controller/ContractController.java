package com.park.sales.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.park.sales.common.Result;
import com.park.sales.dto.ContractCreateRequest;
import com.park.sales.entity.ApprovalTask;
import com.park.sales.entity.Contract;
import com.park.sales.service.ApprovalService;
import com.park.sales.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService contractService;
    private final ApprovalService approvalService;

    public ContractController(ContractService contractService, ApprovalService approvalService) {
        this.contractService = contractService;
        this.approvalService = approvalService;
    }

    @GetMapping
    public Result<IPage<Contract>> page(@RequestParam(defaultValue = "1") long current,
                                        @RequestParam(defaultValue = "10") long size) {
        return Result.ok(contractService.page(current, size));
    }

    @GetMapping("/{id}")
    public Result<Contract> get(@PathVariable Long id) {
        return Result.ok(contractService.get(id));
    }

    @GetMapping("/{id}/approval-tasks")
    public Result<List<ApprovalTask>> approvalTasks(@PathVariable Long id) {
        Contract contract = contractService.get(id);
        if (contract.getApprovalFlowId() == null) {
            return Result.ok(List.of());
        }
        return Result.ok(approvalService.tasksOfFlow(contract.getApprovalFlowId()));
    }

    @PostMapping
    public Result<Contract> create(@Valid @RequestBody ContractCreateRequest req) {
        return Result.ok(contractService.create(req));
    }

    @PostMapping("/{id}/submit")
    public Result<Contract> submit(@PathVariable Long id) {
        return Result.ok(contractService.submit(id));
    }
}

package com.park.sales.controller;

import com.park.sales.common.Result;
import com.park.sales.dto.PaymentCreateRequest;
import com.park.sales.entity.PaymentRecord;
import com.park.sales.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public Result<List<PaymentRecord>> listByContract(@RequestParam Long contractId) {
        return Result.ok(paymentService.listByContract(contractId));
    }

    @PostMapping
    public Result<PaymentRecord> create(@Valid @RequestBody PaymentCreateRequest req) {
        return Result.ok(paymentService.create(req));
    }
}

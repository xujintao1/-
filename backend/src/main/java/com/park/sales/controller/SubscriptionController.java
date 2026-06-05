package com.park.sales.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.park.sales.common.Result;
import com.park.sales.dto.SubscriptionCreateRequest;
import com.park.sales.entity.Subscription;
import com.park.sales.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public Result<IPage<Subscription>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(subscriptionService.page(current, size));
    }

    @PostMapping
    public Result<Subscription> create(@Valid @RequestBody SubscriptionCreateRequest req) {
        return Result.ok(subscriptionService.create(req));
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        subscriptionService.cancel(id);
        return Result.ok();
    }
}

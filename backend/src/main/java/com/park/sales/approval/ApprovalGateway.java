package com.park.sales.approval;

import com.park.sales.entity.ApprovalFlow;

import java.util.List;

/**
 * 审批网关抽象层。
 * 通过实现该接口，可以在「内置审批流」与「对接外部 OA」之间无缝切换，
 * 业务层只依赖本接口，符合开闭原则。
 */
public interface ApprovalGateway {

    /** 网关类型：internal / oa */
    String getType();

    /**
     * 发起审批，创建审批流实例及节点任务。
     *
     * @param bizType 业务类型，如 CONTRACT
     * @param bizId   业务主键
     * @param chain   审批链（按顺序）
     * @return 创建好的审批流
     */
    ApprovalFlow start(String bizType, Long bizId, List<ApprovalNodeDef> chain);

    /**
     * 撤销审批流（发起人主动撤回）。
     * internal 网关无外部动作；oa 网关调用 OA 撤销接口。
     * 本地状态（流程/任务/合同）由 {@code ApprovalService} 统一维护。
     */
    default void cancel(ApprovalFlow flow, String reason) {
    }

    /**
     * 重新提交审批流（撤销或驳回后重新发起）。
     * internal 网关无外部动作；oa 网关调用 OA 重新提交接口。
     */
    default void resubmit(ApprovalFlow flow) {
    }

    /**
     * 删除审批流（含外部 OA 实例）。
     * internal 网关无外部动作；oa 网关调用 OA 删除接口。
     */
    default void delete(ApprovalFlow flow) {
    }
}

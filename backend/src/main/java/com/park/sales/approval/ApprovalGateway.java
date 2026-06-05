package com.park.sales.approval;

import com.park.sales.entity.ApprovalFlow;

import java.util.List;

/**
 * 审批网关抽象层。
 * 通过实现该接口，可在「内置审批流」与「对接外部 OA」之间无缝切换，
 * 业务层只依赖本接口，符合开闭原则。
 */
public interface ApprovalGateway {

    /** 网关类型：internal / oa */
    String getType();

    /**
     * 发起审批，创建审批流实例及各节点任务。
     *
     * @param bizType 业务类型，如 CONTRACT
     * @param bizId   业务主键
     * @param chain   审批链（按顺序）
     * @return 创建好的审批流
     */
    ApprovalFlow start(String bizType, Long bizId, List<ApprovalNodeDef> chain);
}

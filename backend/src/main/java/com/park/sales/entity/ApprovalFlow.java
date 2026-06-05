package com.park.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_flow")
public class ApprovalFlow extends BaseEntity {
    /** 业务类型: CONTRACT */
    private String bizType;
    /** 业务id */
    private Long bizId;
    /** 审批网关: internal/oa */
    private String gateway;
    /** 外部OA实例编号(对接OA时使用) */
    private String externalNo;
    /** 当前审批步骤(从1开始) */
    private Integer currentStep;
    /** 状态: APPROVING/APPROVED/REJECTED */
    private String status;
}

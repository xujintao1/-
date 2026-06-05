package com.park.sales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("approval_task")
public class ApprovalTask extends BaseEntity {
    private Long flowId;
    /** 步骤序号(从1开始) */
    private Integer step;
    /** 审批节点名称 */
    private String nodeName;
    /** 审批所需角色编码 */
    private String approverRole;
    /** 实际审批人id */
    private Long approverId;
    /** 状态: PENDING/APPROVED/REJECTED */
    private String status;
    /** 审批意见 */
    private String comment;
    private LocalDateTime handledTime;

    @TableField(exist = false)
    private String bizTitle;
}

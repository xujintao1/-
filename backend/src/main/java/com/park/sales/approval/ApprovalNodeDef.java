package com.park.sales.approval;

/**
 * 审批节点定义：节点名称 + 所需审批角色编码。
 */
public record ApprovalNodeDef(String nodeName, String role) {
}

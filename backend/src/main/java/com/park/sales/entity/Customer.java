package com.park.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer")
public class Customer extends BaseEntity {
    /** 客户/企业名称 */
    private String name;
    /** 联系人 */
    private String contactPerson;
    private String phone;
    /** 统一社会信用代码 */
    private String creditCode;
    /** 意向等级: A/B/C */
    private String intentLevel;
    private String remark;
    /** 跟进销售用户id */
    private Long ownerId;
}

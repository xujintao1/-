package com.park.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("building")
public class Building extends BaseEntity {
    private Long projectId;
    /** 楼栋名称，如 A栋 */
    private String name;
    private Integer floors;
}

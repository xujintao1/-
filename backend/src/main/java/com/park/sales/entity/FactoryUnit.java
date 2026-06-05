package com.park.sales.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("factory_unit")
public class FactoryUnit extends BaseEntity {
    private Long projectId;
    private Long buildingId;
    /** 厂房单元编号，如 A-101 */
    private String unitNo;
    /** 建筑面积(㎡) */
    private BigDecimal area;
    /** 单价(元/㎡) */
    private BigDecimal unitPrice;
    /** 总价(元) */
    private BigDecimal totalPrice;
    /** 楼层 */
    private Integer floor;
    /** 状态: ON_SALE/SUBSCRIBED/SIGNED/SOLD */
    private String status;

    @TableField(exist = false)
    private String projectName;
    @TableField(exist = false)
    private String buildingName;
}

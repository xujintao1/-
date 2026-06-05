package com.park.sales.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("building_project")
public class BuildingProject extends BaseEntity {
    /** 产业园项目名称 */
    private String name;
    private String address;
    private String description;
}

package com.park.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.park.sales.entity.FactoryUnit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FactoryUnitMapper extends BaseMapper<FactoryUnit> {

    @Select("""
            <script>
            SELECT fu.*, p.name AS projectName, b.name AS buildingName
            FROM factory_unit fu
            LEFT JOIN building_project p ON p.id = fu.project_id
            LEFT JOIN building b ON b.id = fu.building_id
            WHERE fu.deleted = 0
            <if test='status != null and status != ""'> AND fu.status = #{status} </if>
            <if test='keyword != null and keyword != ""'> AND fu.unit_no LIKE CONCAT('%', #{keyword}, '%') </if>
            ORDER BY fu.id DESC
            </script>
            """)
    IPage<FactoryUnit> selectPageWithJoin(Page<FactoryUnit> page,
                                          @Param("status") String status,
                                          @Param("keyword") String keyword);
}

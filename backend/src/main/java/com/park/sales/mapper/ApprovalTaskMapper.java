package com.park.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.park.sales.entity.ApprovalTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApprovalTaskMapper extends BaseMapper<ApprovalTask> {

    @Select("""
            <script>
            SELECT t.*, ct.contract_no AS bizTitle
            FROM approval_task t
            LEFT JOIN approval_flow f ON f.id = t.flow_id
            LEFT JOIN contract ct ON ct.id = f.biz_id
            WHERE t.deleted = 0 AND t.status = 'PENDING'
            AND f.status = 'APPROVING' AND t.step = f.current_step
            <if test='roles != null and roles.size() > 0'>
              AND t.approver_role IN
              <foreach collection='roles' item='r' open='(' separator=',' close=')'>#{r}</foreach>
            </if>
            ORDER BY t.id ASC
            </script>
            """)
    List<ApprovalTask> selectTodo(@Param("roles") List<String> roles);
}

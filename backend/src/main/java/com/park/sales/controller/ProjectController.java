package com.park.sales.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.park.sales.common.Result;
import com.park.sales.entity.Building;
import com.park.sales.entity.BuildingProject;
import com.park.sales.mapper.BuildingMapper;
import com.park.sales.mapper.BuildingProjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final BuildingProjectMapper projectMapper;
    private final BuildingMapper buildingMapper;

    public ProjectController(BuildingProjectMapper projectMapper, BuildingMapper buildingMapper) {
        this.projectMapper = projectMapper;
        this.buildingMapper = buildingMapper;
    }

    @GetMapping
    public Result<List<BuildingProject>> projects() {
        return Result.ok(projectMapper.selectList(new LambdaQueryWrapper<BuildingProject>()
                .orderByDesc(BuildingProject::getId)));
    }

    @GetMapping("/{projectId}/buildings")
    public Result<List<Building>> buildings(@PathVariable Long projectId) {
        return Result.ok(buildingMapper.selectList(new LambdaQueryWrapper<Building>()
                .eq(Building::getProjectId, projectId)
                .orderByAsc(Building::getId)));
    }
}

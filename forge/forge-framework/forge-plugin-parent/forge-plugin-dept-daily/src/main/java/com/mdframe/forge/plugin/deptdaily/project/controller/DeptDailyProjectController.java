package com.mdframe.forge.plugin.deptdaily.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectCreateRequest;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectFinishRequest;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectPageQuery;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectUpdateRequest;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProject;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProjectMember;
import com.mdframe.forge.plugin.deptdaily.project.service.DeptDailyProjectService;
import com.mdframe.forge.plugin.deptdaily.project.vo.ProjectListRowVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/dept-daily/project", "/api/dept-daily/project"})
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class DeptDailyProjectController {

    private final DeptDailyProjectService projectService;

    @GetMapping("/page")
    public RespInfo<IPage<ProjectListRowVO>> page(ProjectPageQuery query) {
        return RespInfo.success(projectService.page(query));
    }

    @GetMapping("/{id}")
    public RespInfo<Map<String, Object>> detail(@PathVariable Long id) {
        DeptDailyProject p = projectService.getById(id);
        if (p == null) return RespInfo.error("项目不存在");
        List<DeptDailyProjectMember> members = projectService.listMembers(id);
        return RespInfo.success(Map.of("project", p, "members", members));
    }

    @PostMapping
    public RespInfo<Long> create(@RequestBody ProjectCreateRequest req) {
        return RespInfo.success(projectService.create(req));
    }

    @PutMapping
    public RespInfo<Void> update(@RequestBody ProjectUpdateRequest req) {
        projectService.update(req);
        return RespInfo.success();
    }

    @PostMapping("/{id}/finish")
    public RespInfo<Void> finish(@PathVariable Long id, @RequestBody ProjectFinishRequest req) {
        projectService.finish(id, req);
        return RespInfo.success();
    }

    /**
     * 个人月报填报用：未截止且本人参与/负责的项目
     */
    @GetMapping("/fillable")
    public RespInfo<List<DeptDailyProject>> fillable(@RequestParam(required = false) Long deptId,
                                                     @RequestParam(required = false) Long officeId) {
        return RespInfo.success(projectService.listFillableProjects(deptId, officeId));
    }
}


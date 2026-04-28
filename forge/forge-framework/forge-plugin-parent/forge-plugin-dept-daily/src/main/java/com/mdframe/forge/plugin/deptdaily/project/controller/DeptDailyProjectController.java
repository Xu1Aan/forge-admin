package com.mdframe.forge.plugin.deptdaily.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectCreateRequest;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectExcelImportResult;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectExcelImportRow;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectFinishRequest;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectPageQuery;
import com.mdframe.forge.plugin.deptdaily.project.dto.ProjectUpdateRequest;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProject;
import com.mdframe.forge.plugin.deptdaily.project.entity.DeptDailyProjectMember;
import com.mdframe.forge.plugin.deptdaily.project.service.DeptDailyProjectService;
import com.mdframe.forge.plugin.deptdaily.project.vo.ProjectListRowVO;
import com.mdframe.forge.plugin.deptdaily.project.vo.ProjectMemberRowVO;
import com.alibaba.excel.EasyExcel;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
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
        Map<String, Object> data = new HashMap<>(8);
        data.put("project", p);
        data.put("members", members);
        data.put("memberRows", projectService.listMemberRows(id));
        return RespInfo.success(data);
    }

    /**
     * 按用户 ID 列表返回展示用简报（姓名、用户名、部门、电话等），顺序与入参一致。
     */
    @PostMapping("/users/brief")
    public RespInfo<List<ProjectMemberRowVO>> usersBrief(@RequestBody(required = false) List<Long> userIds) {
        if (userIds == null) {
            userIds = List.of();
        }
        return RespInfo.success(projectService.listMemberBriefsOrdered(userIds));
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
     * 删除项目（谨慎）：仅允许删除未产生任何月报数据的项目。
     */
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id, @RequestParam(required = false) String reason) {
        projectService.deleteProject(id, reason);
        return RespInfo.success();
    }

    /**
     * Excel导入：项目名/类型/负责人/成员（按姓名匹配 sys_user.real_name 或 username）。
     * <p>
     * 为避免误导入，默认 dryRun=true，仅校验；传 dryRun=false 才会落库。
     */
    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RespInfo<ProjectExcelImportResult> importExcel(@RequestPart("file") MultipartFile file,
                                                          @RequestParam(defaultValue = "true") boolean dryRun,
                                                          @RequestParam(required = false) String defaultStartDate,
                                                          @RequestParam(required = false) String defaultPlanEndDate) {
        if (file == null || file.isEmpty()) {
            return RespInfo.error("请上传Excel文件");
        }
        LocalDate start = defaultStartDate != null && !defaultStartDate.isBlank()
                ? LocalDate.parse(defaultStartDate.trim())
                : LocalDate.now();
        LocalDate end = defaultPlanEndDate != null && !defaultPlanEndDate.isBlank()
                ? LocalDate.parse(defaultPlanEndDate.trim())
                : start.plusMonths(6);

        try (InputStream in = file.getInputStream()) {
            List<ProjectExcelImportRow> rows = EasyExcel.read(in)
                    .head(ProjectExcelImportRow.class)
                    .sheet()
                    .doReadSync();
            return RespInfo.success(projectService.importProjectsFromExcelRows(rows, dryRun, start, end));
        } catch (Exception e) {
            return RespInfo.error("导入失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }
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


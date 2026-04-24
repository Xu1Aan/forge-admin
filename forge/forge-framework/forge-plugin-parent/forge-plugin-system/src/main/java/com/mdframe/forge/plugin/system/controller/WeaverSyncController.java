package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.entity.SysExternalSyncBatch;
import com.mdframe.forge.plugin.system.external.weaver.WeaverClient;
import com.mdframe.forge.plugin.system.external.weaver.WeaverSyncResult;
import com.mdframe.forge.plugin.system.external.weaver.WeaverSyncService;
import com.mdframe.forge.plugin.system.mapper.SysExternalSyncBatchMapper;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 泛微用户/组织同步管理接口
 * <p>
 * 注意：当前系统控制器普遍标记了 {@link ApiPermissionIgnore}，此处沿用现有风格；
 * 若你们后续启用更严格的API权限，请在网关/接口权限层加白名单或管理员权限控制。
 */
@RestController
@RequestMapping("/system/external/weaver")
@RequiredArgsConstructor
@ApiPermissionIgnore
public class WeaverSyncController {
    private final WeaverSyncService syncService;
    private final SysExternalSyncBatchMapper batchMapper;

    /**
     * 提交一次全量差分同步（手动）：立即返回 running 与 batchId，实际拉取与写库在后台执行；进度与结果以批次表为准。
     */
    @PostMapping("/sync")
    public RespInfo<WeaverSyncResult> sync() {
        WeaverSyncResult result = syncService.startAsyncWeaverSync();
        return RespInfo.success(result);
    }

    /**
     * 分页查看同步批次
     */
    @GetMapping("/batches")
    public RespInfo<IPage<SysExternalSyncBatch>> batches(@RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "20") long pageSize) {
        LambdaQueryWrapper<SysExternalSyncBatch> q = new LambdaQueryWrapper<SysExternalSyncBatch>()
                .eq(SysExternalSyncBatch::getPlatform, WeaverClient.PLATFORM)
                .orderByDesc(SysExternalSyncBatch::getStartedAt);
        IPage<SysExternalSyncBatch> page = batchMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return RespInfo.success(page);
    }
}


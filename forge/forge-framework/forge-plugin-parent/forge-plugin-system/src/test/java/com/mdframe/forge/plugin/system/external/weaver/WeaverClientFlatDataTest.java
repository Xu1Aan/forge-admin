package com.mdframe.forge.plugin.system.external.weaver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.external.weaver.model.FlatRow;
import com.mdframe.forge.plugin.system.external.weaver.model.GetUsersInfoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WeaverClientFlatDataTest {

    @Test
    void normalizeFlat_mapsDepartmentUserAndWorkcode() throws Exception {
        WeaverProperties props = new WeaverProperties();
        props.setOrgDisabledNameContains("已封存");
        WeaverClient client = new WeaverClient(null, new ObjectMapper(), props);

        try (InputStream in = new ClassPathResource("weaver/getusersinfo-sample.json").getInputStream()) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            GetUsersInfoResponse resp = new ObjectMapper().readValue(json, GetUsersInfoResponse.class);
            WeaverClient.NormalizeResult r = client.normalizeFlat(resp.getData());
            assertEquals(1, r.getOrgs().size());
            assertEquals("2", r.getOrgs().get(0).getExternalOrgId());
            assertNull(r.getOrgs().get(0).getExternalParentId());
            assertEquals("1", r.getOrgs().get(0).getStatus());

            assertEquals(1, r.getUsers().size());
            assertEquals("0001", r.getUsers().get(0).getExternalUserId());
            assertEquals("22", r.getUsers().get(0).getResourceId());
            assertEquals("1", r.getUsers().get(0).getStatus());
            assertEquals(0, r.getSkippedNoExternalUserId());
        }
    }

    @Test
    void normalizeFlat_disablesOrgWhenNameContainsMark() {
        WeaverProperties props = new WeaverProperties();
        props.setOrgDisabledNameContains("已封存");
        WeaverClient client = new WeaverClient(null, null, props);

        FlatRow row = new FlatRow();
        row.setRowKind("department");
        row.setDepartmentId("14");
        row.setSupDepartmentId("0");
        row.setDepartmentName("工程设计一分院[已封存]");

        WeaverClient.NormalizeResult r = client.normalizeFlat(java.util.List.of(row));
        assertEquals(1, r.getOrgs().size());
        assertEquals("0", r.getOrgs().get(0).getStatus());
    }
}

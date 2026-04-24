package com.mdframe.forge.plugin.system.external.weaver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.external.weaver.model.WeaverSyncPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeaverClientNormalizeTest {

    @Test
    void normalize_shouldExtractUsersAndDeptFallbackOrgs() throws Exception {
        String json = """
                {
                  "children": [
                    {
                      "account": "A000884",
                      "deptId": "2",
                      "deptName": "办公室",
                      "mobile": "13808219375",
                      "email": "",
                      "name": "王刚强",
                      "nodeType": "user",
                      "status": "1",
                      "updateTime": 1776670156107
                    }
                  ]
                }
                """;

        ObjectMapper om = new ObjectMapper();
        WeaverSyncPayload payload = om.readValue(json, WeaverSyncPayload.class);

        WeaverClient.NormalizeResult r = new WeaverClient(null, null, null).normalize(payload);
        assertEquals(1, r.getUsers().size());
        assertEquals(1, r.getOrgs().size());
        assertEquals("2", r.getOrgs().get(0).getExternalOrgId());
        assertEquals("办公室", r.getOrgs().get(0).getName());
        assertEquals("A000884", r.getUsers().get(0).getExternalUserId());
        assertEquals("2", r.getUsers().get(0).getDeptExternalId());
        assertEquals(0, r.getSkippedNoExternalUserId());
    }

    @Test
    void normalize_shouldExtractOrgNodesWhenProvided() throws Exception {
        String json = """
                {
                  "children": [
                    { "nodeType": "org", "orgId": "1", "orgName": "总部", "parentId": "0", "orgStatus": "1",
                      "children": [
                        { "nodeType": "org", "orgId": "2", "orgName": "办公室", "parentId": "1", "orgStatus": "1" }
                      ]
                    }
                  ]
                }
                """;
        ObjectMapper om = new ObjectMapper();
        WeaverSyncPayload payload = om.readValue(json, WeaverSyncPayload.class);

        WeaverClient.NormalizeResult r = new WeaverClient(null, null, null).normalize(payload);
        assertEquals(2, r.getOrgs().size());
        assertTrue(r.getOrgs().stream().anyMatch(o -> "1".equals(o.getExternalOrgId()) && "总部".equals(o.getName())));
        assertTrue(r.getOrgs().stream().anyMatch(o -> "2".equals(o.getExternalOrgId()) && "办公室".equals(o.getName()) && "1".equals(o.getExternalParentId())));
        assertEquals(0, r.getSkippedNoExternalUserId());
    }
}


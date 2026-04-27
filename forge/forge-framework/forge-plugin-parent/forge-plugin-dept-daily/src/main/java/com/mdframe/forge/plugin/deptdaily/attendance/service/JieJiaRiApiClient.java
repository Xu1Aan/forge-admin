package com.mdframe.forge.plugin.deptdaily.attendance.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 第三方节假日接口客户端（https://api.jiejiariapi.com）
 */
@Component
@RequiredArgsConstructor
public class JieJiaRiApiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public Map<String, DayInfo> fetchHolidays(int year) {
        String url = "https://api.jiejiariapi.com/v1/holidays/" + year;
        return fetchMap(url);
    }

    public Map<String, DayInfo> fetchWeekends(int year) {
        String url = "https://api.jiejiariapi.com/v1/weekends/" + year;
        return fetchMap(url);
    }

    private Map<String, DayInfo> fetchMap(String url) {
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
        String body = resp.getBody();
        if (StringUtils.isBlank(body)) {
            return new HashMap<>();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            Map<String, DayInfo> map = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> it = root.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                DayInfo d = objectMapper.treeToValue(e.getValue(), DayInfo.class);
                map.put(e.getKey(), d);
            }
            return map;
        } catch (Exception e) {
            throw new IllegalStateException("解析节假日接口响应失败: " + e.getMessage(), e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DayInfo {
        private String date;
        private String name;
        private Boolean isOffDay;
    }
}


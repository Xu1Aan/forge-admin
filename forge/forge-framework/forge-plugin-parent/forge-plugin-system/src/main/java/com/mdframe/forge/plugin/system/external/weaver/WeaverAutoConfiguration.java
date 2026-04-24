package com.mdframe.forge.plugin.system.external.weaver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(WeaverProperties.class)
public class WeaverAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate weaverRestTemplate(WeaverProperties properties) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(properties.getTimeoutMs());
        rf.setReadTimeout(properties.getTimeoutMs());
        return new RestTemplate(rf);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper weaverObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}


package com.mdframe.forge.starter.auth.sso.weaver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdframe.forge.starter.auth.context.WeaverSsoProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 泛微 SSO 自动配置
 */
@AutoConfiguration
@EnableConfigurationProperties(WeaverSsoProperties.class)
public class WeaverSsoAutoConfiguration {

    @Bean(name = {"weaverRestTemplate", "weaverSsoRestTemplate"})
    @ConditionalOnMissingBean(name = "weaverRestTemplate")
    public RestTemplate weaverRestTemplate() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(10000);
        rf.setReadTimeout(10000);
        return new RestTemplate(rf);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper weaverSsoObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}


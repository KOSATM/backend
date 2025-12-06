package com.example.demo.config;


import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class TimeOutConfig {

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder
            .requestFactory(new SimpleClientHttpRequestFactory() {{
                setReadTimeout(60 * 1000);    // 읽기 타임아웃: 60초 (원하는 시간으로 조절)
                setConnectTimeout(10 * 1000); // 연결 타임아웃: 10초
            }});
    }
}
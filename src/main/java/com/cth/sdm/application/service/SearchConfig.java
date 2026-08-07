package com.cth.sdm.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@RequiredArgsConstructor
public class SearchConfig {

    private final DatabaseFallbackSearchService databaseFallbackSearchService;
    private final ElasticsearchSearchService elasticsearchSearchService;

    @Value("${app.search.elasticsearch-enabled:false}")
    private boolean elasticsearchEnabled;

    @Bean
    @Primary
    public SearchService searchService() {
        if (elasticsearchEnabled) {
            return elasticsearchSearchService;
        }
        return databaseFallbackSearchService;
    }
}

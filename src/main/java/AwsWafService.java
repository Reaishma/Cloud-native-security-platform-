package com.cloudnative.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.wafv2.Wafv2Client;
import software.amazon.awssdk.services.wafv2.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;


@Service
public class AwsWafService {
    private static final Logger logger = LoggerFactory.getLogger(AwsWafService.class);
    private Wafv2Client wafClient;

    @PostConstruct
    public void init() {
        try {
            this.wafClient = Wafv2Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
            logger.info("AWS WAF client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize AWS WAF client", e);
        }
    }

    public void createWebACL(String webAclName) {
        try {
            // SQL Injection protection rule
            Rule sqlInjectionRule = Rule.builder()
                .name("SQLInjectionRule")
                .priority(1)
                .action(RuleAction.builder().block(BlockAction.builder().build()).build())
                .statement(Statement.builder()
                    .sqliMatchStatement(SqliMatchStatement.builder()
                        .fieldToMatch(FieldToMatch.builder()
                            .allQueryArguments(AllQueryArguments.builder().build())
                            .build())
                        .textTransformations(TextTransformation.builder()
                            .priority(1)
                            .type(TextTransformationType.URL_DECODE)
                            .build())
                        .build())
                    .build())
                .visibilityConfig(VisibilityConfig.builder()
                    .sampledRequestsEnabled(true)
                    .cloudWatchMetricsEnabled(true)
                    .metricName("SQLInjectionRule")
                    .build())
                .build();

            // XSS protection rule
            Rule xssRule = Rule.builder()
                .name("XSSRule")
                .priority(2)
                .action(RuleAction.builder().block(BlockAction.builder().build()).build())
                .statement(Statement.builder()
                    .xssMatchStatement(XssMatchStatement.builder()
                        .fieldToMatch(FieldToMatch.builder()
                            .body(Body.builder().build())
                            .build())
                        .textTransformations(TextTransformation.builder()
                            .priority(1)
                            .type(TextTransformationType.HTML_ENTITY_DECODE)
                            .build())
                        .build())
                    .build())
                .visibilityConfig(VisibilityConfig.builder()
                    .sampledRequestsEnabled(true)
                    .cloudWatchMetricsEnabled(true)
                    .metricName("XSSRule")
                    .build())
                .build();

            CreateWebAclRequest request = CreateWebAclRequest.builder()
                .name(webAclName)
                .scope(Scope.REGIONAL)
                .defaultAction(DefaultAction.builder().allow(AllowAction.builder().build()).build())
                .rules(Arrays.asList(sqlInjectionRule, xssRule))
                .visibilityConfig(VisibilityConfig.builder()
                    .sampledRequestsEnabled(true)
                    .cloudWatchMetricsEnabled(true)
                    .metricName(webAclName)
                    .build())
                .build();

            CreateWebAclResponse response = wafClient.createWebACL(request);
            logger.info("Created Web ACL: {} with ID: {}", webAclName, response.summary().id());
        } catch (Exception e) {
            logger.error("Failed to create Web ACL", e);
            throw new RuntimeException("Failed to create Web ACL", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (wafClient != null) {
            wafClient.close();
        }
    }
}
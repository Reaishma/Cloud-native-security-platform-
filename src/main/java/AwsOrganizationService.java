package com.cloudnative.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.organizations.OrganizationsClient;
import software.amazon.awssdk.services.organizations.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AwsOrganizationService {
    private static final Logger logger = LoggerFactory.getLogger(AwsOrganizationService.class);
    private OrganizationsClient organizationsClient;

    @PostConstruct
    public void init() {
        try {
            this.organizationsClient = OrganizationsClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
            logger.info("AWS Organizations client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize AWS Organizations client", e);
        }
    }

    public List<String> listAccounts() {
        try {
            ListAccountsResponse response = organizationsClient.listAccounts();
            return response.accounts().stream()
                .map(account -> account.name() + " (" + account.id() + ")")
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to list accounts", e);
            throw new RuntimeException("Failed to list accounts", e);
        }
    }

    public void createPolicy(String policyName, String policyDocument, PolicyType policyType) {
        try {
            CreatePolicyRequest request = CreatePolicyRequest.builder()
                .name(policyName)
                .description("Security policy for cloud-native application")
                .type(policyType)
                .content(policyDocument)
                .build();

            CreatePolicyResponse response = organizationsClient.createPolicy(request);
            logger.info("Created policy: {} with ID: {}", policyName, response.policy().policySummary().id());
        } catch (Exception e) {
            logger.error("Failed to create policy", e);
            throw new RuntimeException("Failed to create policy", e);
        }
    }

    public String getSecurityPolicyDocument() {
        return """
        {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Deny",
                    "Action": [
                        "ec2:TerminateInstances",
                        "rds:DeleteDBInstance"
                    ],
                    "Resource": "*",
                    "Condition": {
                        "StringNotEquals": {
                            "aws:PrincipalTag/Department": "Security"
                        }
                    }
                }
            ]
        }
        """;
    }

    @PreDestroy
    public void cleanup() {
        if (organizationsClient != null) {
            organizationsClient.close();
        }
    }
}
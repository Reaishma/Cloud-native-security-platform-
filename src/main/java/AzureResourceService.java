
package com.cloudnative.service;

package com.cloudnative.service;

import com.azure.core.credential.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AzureResourceService {
    private static final Logger logger = LoggerFactory.getLogger(AzureResourceService.class);
    private AzureResourceManager azure;
    
    @PostConstruct
    public void init() {
        try {
            this.azure = AzureResourceManager
                .authenticate(new DefaultAzureCredentialBuilder().build(), 
                             System.getenv("AZURE_SUBSCRIPTION_ID"))
                .withDefaultSubscription();
            logger.info("Azure Resource Manager initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Azure Resource Manager", e);
        }
    }
    
    public List<String> listResourceGroups() {
        try {
            return azure.resourceGroups().list()
                .stream()
                .map(ResourceGroup::name)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to list resource groups", e);
            throw new RuntimeException("Failed to list resource groups", e);
        }
    }
    
    public void createResourceGroup(String name, String region) {
        try {
            azure.resourceGroups()
                .define(name)
                .withRegion(region)
                .create();
            logger.info("Created resource group: {} in region: {}", name, region);
        } catch (Exception e) {
            logger.error("Failed to create resource group", e);
            throw new RuntimeException("Failed to create resource group", e);
        }
    }
    
    public void deleteResourceGroup(String name) {
        try {
            azure.resourceGroups().deleteByName(name);
            logger.info("Deleted resource group: {}", name);
        } catch (Exception e) {
            logger.error("Failed to delete resource group", e);
            throw new RuntimeException("Failed to delete resource group", e);
        }
    }
}


package com.cloudnative.controller;

import com.cloudnative.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Validated
public class CloudNativeController {
    private static final Logger logger = LoggerFactory.getLogger(CloudNativeController.class);
    
    @Autowired
    private AwsKmsService kmsService;
    
    @Autowired
    private AzureResourceService azureService;
    
    @Autowired
    private AwsOrganizationService organizationService;
    
    @Autowired
    private AwsWafService wafService;
    
    @Autowired
    private MonitoringService monitoringService;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        long startTime = System.currentTimeMillis();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        response.put("service", "cloud-native-app");
        
        long duration = System.currentTimeMillis() - startTime;
        monitoringService.logPerformanceMetrics("health_check", duration, true);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/encrypt")
    public ResponseEntity<Map<String, String>> encryptData(
            @RequestBody @NotBlank @Size(max = 1000) String data) {
        long startTime = System.currentTimeMillis();
        
        try {
            String encryptedData = kmsService.encryptData(data);
            
            Map<String, String> response = new HashMap<>();
            response.put("encryptedData", encryptedData);
            response.put("status", "success");
            
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.logPerformanceMetrics("encrypt_data", duration, true);
            monitoringService.logSecurityEvent("DATA_ENCRYPTED", "system", "Data encrypted using AWS KMS");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.logPerformanceMetrics("encrypt_data", duration, false);
            monitoringService.logSecurityEvent("ENCRYPTION_FAILED", "system", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Encryption failed");
            errorResponse.put("status", "error");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/decrypt")
    public ResponseEntity<Map<String, String>> decryptData(
            @RequestBody @NotBlank String encryptedData) {
        long startTime = System.currentTimeMillis();
        
        try {
            String decryptedData = kmsService.decryptData(encryptedData);
            
            Map<String, String> response = new HashMap<>();
            response.put("decryptedData", decryptedData);
            response.put("status", "success");
            
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.logPerformanceMetrics("decrypt_data", duration, true);
            monitoringService.logSecurityEvent("DATA_DECRYPTED", "system", "Data decrypted using AWS KMS");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.logPerformanceMetrics("decrypt_data", duration, false);
            monitoringService.logSecurityEvent("DECRYPTION_FAILED", "system", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Decryption failed");
            errorResponse.put("status", "error");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/azure/resource-groups")
    public ResponseEntity<List<String>> listAzureResourceGroups() {
        long startTime = System.currentTimeMillis();
        
        try {
            List<String> resourceGroups = azureService.listResourceGroups();
            
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.logPerformanceMetrics("list_azure_resources", duration, true);
            
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("resourceGroupCount", resourceGroups.size());
            monitoringService.logApplicationEvent("AZURE_RESOURCES_LISTED", eventData);
            
            return ResponseEntity.ok(resourceGroups);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.logPerformanceMetrics("list_azure_resources", duration, false);
            
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/aws/accounts")
    public ResponseEntity<List<String>> listAwsAccounts() {
        long startTime = System.currentTimeMillis();
        
        try {
            List<String> accounts = organizationService.listAccounts();
            
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.logPerformanceMetrics("list_aws_accounts", duration, true);
            
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("accountCount", accounts.size());
            monitoringService.logApplicationEvent("AWS_ACCOUNTS_LISTED", eventData);
            
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.logPerformanceMetrics("list_aws_accounts", duration, false);
            
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/security/waf/create")
    public ResponseEntity<Map<String, String>> createWebACL(
            @RequestBody @NotBlank String webAclName) {
        long startTime = System.currentTimeMillis();
        
        try {
            wafService.createWebACL(webAclName);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Web ACL created successfully");
            response.put("webAclName", webAclName);
            response.put("status", "success");
            
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.logPerformanceMetrics("create_web_acl", duration, true);
            monitoringService.logSecurityEvent("WAF_CREATED", "system", "Web ACL created: " + webAclName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            monitoringService.logPerformanceMetrics("create_web_acl", duration, false);
            monitoringService.logSecurityEvent("WAF_CREATION_FAILED", "system", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create Web ACL");
            errorResponse.put("status", "error");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/security/test-injection")
    public ResponseEntity<Map<String, String>> testSqlInjection(
            @RequestParam String query) {
        // This endpoint demonstrates SQL injection detection
        if (query.toLowerCase().contains("drop") || 
            query.toLowerCase().contains("delete") || 
            query.toLowerCase().contains("union")) {
            
            monitoringService.logSecurityEvent("SQL_INJECTION_ATTEMPT", "unknown", 
                "Malicious query detected: " + query);
            
            Map<String, String> response = new HashMap<>();
            response.put("error", "Malicious query detected");
            response.put("status", "blocked");
            return ResponseEntity.badRequest().body(response);
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Query processed safely");
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
}

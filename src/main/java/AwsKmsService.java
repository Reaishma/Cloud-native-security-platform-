
package com.cloudnative.service;

package com.cloudnative.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;
import software.amazon.awssdk.core.SdkBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class AwsKmsService {
    private static final Logger logger = LoggerFactory.getLogger(AwsKmsService.class);
    private KmsClient kmsClient;
    private String keyId = "alias/cloud-native-app-key";
    
    @PostConstruct
    public void init() {
        try {
            this.kmsClient = KmsClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
            logger.info("AWS KMS client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize AWS KMS client", e);
        }
    }
    
    public String encryptData(String plaintext) {
        try {
            EncryptRequest request = EncryptRequest.builder()
                .keyId(keyId)
                .plaintext(SdkBytes.fromString(plaintext, StandardCharsets.UTF_8))
                .build();
            
            EncryptResponse response = kmsClient.encrypt(request);
            String encryptedData = Base64.getEncoder().encodeToString(response.ciphertextBlob().asByteArray());
            
            logger.info("Data encrypted successfully");
            return encryptedData;
        } catch (Exception e) {
            logger.error("Failed to encrypt data", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    public String decryptData(String encryptedData) {
        try {
            byte[] ciphertextBlob = Base64.getDecoder().decode(encryptedData);
            
            DecryptRequest request = DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(ciphertextBlob))
                .build();
            
            DecryptResponse response = kmsClient.decrypt(request);
            String decryptedData = response.plaintext().asString(StandardCharsets.UTF_8);
            
            logger.info("Data decrypted successfully");
            return decryptedData;
        } catch (Exception e) {
            logger.error("Failed to decrypt data", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    public void createKey(String description) {
        try {
            CreateKeyRequest request = CreateKeyRequest.builder()
                .description(description)
                .usage(KeyUsageType.ENCRYPT_DECRYPT)
                .keySpec(KeySpec.SYMMETRIC_DEFAULT)
                .build();
            
            CreateKeyResponse response = kmsClient.createKey(request);
            logger.info("Created KMS key: {}", response.keyMetadata().keyId());
        } catch (Exception e) {
            logger.error("Failed to create KMS key", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (kmsClient != null) {
            kmsClient.close();
        }
    }
}

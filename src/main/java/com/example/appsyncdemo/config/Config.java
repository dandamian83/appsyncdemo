package com.example.appsyncdemo.config;

import com.example.appsyncdemo.aws.AppSyncUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class Config {

    @Bean
    public AppSyncUtil getAppSyncUtil(@Value("${AWS_ACCESS_KEY}") String accessKey,
                                      @Value("${AWS_SECRET_KEY}") String secretKey,
                                      @Value("${AWS_REGION}") String region,
                                      @Value("${AWS_APP_SYNC_URL}") String appSyncUrl,
                                      @Value("${AWS_APP_SYNC_USER_POOL_ID}") String userPoolId,
                                      @Value("${AWS_APP_SYNC_CLIENT_APP_ID}") String clientAppId,
                                      @Value("${AWS_COGNITO_USERNAME}") String username,
                                      @Value("${AWS_COGNITO_PASSWORD}") String password) {

        return new AppSyncUtil(accessKey, secretKey, region, appSyncUrl, userPoolId, clientAppId, username, password);
    }
}

package com.example.appsyncdemo.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthResult;
import com.amazonaws.services.cognitoidp.model.AuthFlowType;
import com.amazonaws.services.cognitoidp.model.AuthenticationResultType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Configuration
public class Config {

//    @Bean
//    public WebClient.RequestBodySpec getWebClient( @Value("${AWS_APP_SYNC_URL}") String appSyncUrl, @Value("${AWS_APP_SYNC_API_KEY}") String apiKey) {
//        return WebClient
//                .builder()
//                .baseUrl(appSyncUrl)
//                .defaultHeader("x-api-key", apiKey)
//                .build()
//                .method(HttpMethod.POST)
//                .uri("");
//    }

    @Bean
    public WebClient.RequestBodySpec createCognitoClient(@Value("${AWS_ACCESS_KEY}") String accessKey,
                                                         @Value("${AWS_SECRET_KEY}") String secretKey,
                                                         @Value("${AWS_REGION}") String region,
                                                         @Value("${AWS_APP_SYNC_URL}") String appSyncUrl) {
        AWSCredentials cred = new BasicAWSCredentials(accessKey, secretKey);
        AWSCredentialsProvider credProvider = new AWSStaticCredentialsProvider(cred);
        AWSCognitoIdentityProvider x = AWSCognitoIdentityProviderClientBuilder.standard()
                .withCredentials(credProvider)
                .withRegion(region)
                .build();

        Map<String, String> authParams = new LinkedHashMap<>() {{
            put("USERNAME", "backend");
            put("PASSWORD", "P@ssw0rd");
        }};

        AdminInitiateAuthRequest authRequest = new AdminInitiateAuthRequest()
                .withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                .withUserPoolId("eu-central-1_29CFknuNU")
                .withClientId("2i1l8vc0nl3eba5h8m6t2buftr")
                .withAuthParameters(authParams);

        AdminInitiateAuthResult authResult = x.adminInitiateAuth(authRequest);
        AuthenticationResultType resultType = authResult.getAuthenticationResult();

        log.debug("idToken {}", resultType.getIdToken());
        log.debug("accessToken {}", resultType.getAccessToken());
        log.debug("refreshToken {}", resultType.getRefreshToken());
        log.debug("message {}", "Successfully login");

        return WebClient
                .builder()
                .baseUrl(appSyncUrl)
                .defaultHeader("Authorization", resultType.getIdToken())
                .build()
                .method(HttpMethod.POST)
                .uri("");
    }
}

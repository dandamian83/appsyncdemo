package com.example.appsyncdemo.aws;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.InitiateAuthResult;
import com.amazonaws.services.cognitoidp.model.NotAuthorizedException;
import com.example.appsyncdemo.client.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
public class AppSyncUtil {

    private final int MAX_TRIES = 3;
    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final String clientAppId;
    private final String appSyncUrl;
    private final String userPoolId;
    private final String username;
    private final String password;
    private AWSCognitoIdentityProvider client;
    private WebClient.RequestBodySpec requestBodySpec;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String idToken;
    private String accessToken;
    private String refreshToken;

    public AppSyncUtil(String accessKey,
                       String secretKey,
                       String region,
                       String appSyncUrl,
                       String userPoolId,
                       String clientAppId,
                       String username,
                       String password) {

        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
        this.clientAppId = clientAppId;
        this.appSyncUrl = appSyncUrl;
        this.userPoolId = userPoolId;
        this.username = username;
        this.password = password;


        initAWSCognitoIdentityProvider();
        initCognitoClient();
    }


    public void push(Notification notification, String channel) throws Exception {

        String escapedJsonString = objectMapper.writeValueAsString(objectMapper.writeValueAsString(notification));

        Map<String, Object> requestBody = new HashMap<>();
        String queryString =
            "mutation add {" + "    publish(" + "        data:" + escapedJsonString + "," + "         name: \"" + channel + "\"" + "    ){" + "        data"
            + "        name" + "    }" + "}";
        log.debug("queryString {}", queryString);
        requestBody.put("query", queryString);

        String bodyString = null;

        int tryings = 0;
        boolean success = false;
        while (tryings < MAX_TRIES && !success) {
            try {
                WebClient.ResponseSpec response = requestBodySpec.body(BodyInserters.fromValue(requestBody))
                                                                 .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                                                                 .acceptCharset(StandardCharsets.UTF_8)
                                                                 .retrieve();
                bodyString = response.bodyToMono(String.class)
                                     .block();
                success = true;
            } catch (Exception e) {
                tryings++;
                if (e instanceof WebClientResponseException.Unauthorized) {
                    log.error("Expired [ access ] token");
                    refreshAccessTokens();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(idToken);
                    requestBodySpec = WebClient.builder()
                                               .baseUrl(appSyncUrl)
                                               .defaultHeader("Authorization", idToken)
                                               .build()
                                               .method(HttpMethod.POST)
                                               .uri("");
                } else {
                    log.error("Exception class {}", e.getClass());
                    log.error("Exception on mutation {}", e.getMessage());
                }
            }
        }

        log.debug("notification sent, body {}", bodyString);
    }

    public void refreshAccessTokens() throws Exception {
        int tryings = 0;
        boolean success = false;
        while (tryings < MAX_TRIES && !success) {
            try {
                Map<String, String> authParams = new LinkedHashMap<>() {{
                    put("REFRESH_TOKEN", refreshToken);
                }};
                InitiateAuthRequest refreshAccessTokensReq = new InitiateAuthRequest().withAuthFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                                                                                      .withClientId(clientAppId)
                                                                                      .withAuthParameters(authParams);
                InitiateAuthResult authResult = client.initiateAuth(refreshAccessTokensReq);
                AuthenticationResultType resultType = authResult.getAuthenticationResult();
                idToken = resultType.getIdToken();
                accessToken = resultType.getAccessToken();
                logToken("refreshAccessTokens", "idToken", idToken);
                logToken("refreshAccessTokens", "accessToken", accessToken);
                success = true;
            } catch (Exception e) {
                tryings++;
                if (e instanceof NotAuthorizedException) {
                    log.error("Expired [ refresh ] token");
                    success = refreshAllTokens();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setBearerAuth(idToken);
                    requestBodySpec = WebClient.builder()
                                               .baseUrl(appSyncUrl)
                                               .defaultHeader("Authorization", idToken)
                                               .build()
                                               .method(HttpMethod.POST)
                                               .uri("");
                } else {
                    log.error("Exception class {}", e.getClass());
                    log.error("Exception on mutation {}", e.getMessage());
                }
            }
        }

        if (!success) {
            throw new Exception("Could not refresh access tokens.");
        }
    }

    public boolean refreshAllTokens() {
        try {
            Map<String, String> authParams = new LinkedHashMap<>() {{
                put("USERNAME", username);
                put("PASSWORD", password);
            }};

            AdminInitiateAuthRequest authRequest = new AdminInitiateAuthRequest().withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                    .withUserPoolId(userPoolId)
                    .withClientId(clientAppId)
                    .withAuthParameters(authParams);

            AdminInitiateAuthResult authResult = client.adminInitiateAuth(authRequest);

            AuthenticationResultType resultType = authResult.getAuthenticationResult();
            idToken = resultType.getIdToken();
            accessToken = resultType.getAccessToken();
            refreshToken = resultType.getRefreshToken();

            logToken("refreshAllTokens", "idToken", idToken);
            logToken("refreshAllTokens", "accessToken", accessToken);
            logToken("refreshAllTokens", "refreshToken", refreshToken);

            return true;
        } catch (Exception e) {
            log.error("Exception", e);
        }

        return false;

    }

    private void initAWSCognitoIdentityProvider() {
        AWSCredentials cred = new BasicAWSCredentials(accessKey, secretKey);
        AWSCredentialsProvider credProvider = new AWSStaticCredentialsProvider(cred);
        client = AWSCognitoIdentityProviderClientBuilder.standard()
                                                        .withCredentials(credProvider)
                                                        .withRegion(region)
                                                        .build();
    }

    private void initCognitoClient() {
        Map<String, String> authParams = new LinkedHashMap<>() {{
            put("USERNAME", username);
            put("PASSWORD", password);
        }};

        AdminInitiateAuthRequest authRequest = new AdminInitiateAuthRequest().withAuthFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                                                                             .withUserPoolId(userPoolId)
                                                                             .withClientId(clientAppId)
                                                                             .withAuthParameters(authParams);

        AdminInitiateAuthResult authResult = client.adminInitiateAuth(authRequest);

        AuthenticationResultType resultType = authResult.getAuthenticationResult();

        idToken = resultType.getIdToken();
        accessToken = resultType.getAccessToken();
        refreshToken = resultType.getRefreshToken();

        logToken("initCognitoClient", "idToken", idToken);
        logToken("initCognitoClient", "accessToken", accessToken);
        logToken("initCognitoClient", "refreshToken", refreshToken);


        requestBodySpec = WebClient.builder()
                                   .baseUrl(appSyncUrl)
                                   .defaultHeader("Authorization", resultType.getIdToken())
                                   .build()
                                   .method(HttpMethod.POST)
                                   .uri("");

        log.debug("successfully init");
    }

    private void logToken(String function, String tokenName, String tokenValue) {
        String tokenShort = Optional.ofNullable(tokenValue)
                                    .map(t -> t.substring(0, 4) + "..." + t.substring(t.length() - 4, t.length()))
                                    .orElse("?");
        log.debug("{}: [ {} > {} ]", function, tokenName, tokenShort);
    }
}

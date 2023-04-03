package com.example.appsyncdemo.controller;

import com.example.appsyncdemo.client.Notification;
import com.example.appsyncdemo.client.SomeRequest;
import com.example.appsyncdemo.client.SomeResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("main")
public class MainController {

    @Autowired
    WebClient.RequestBodySpec requestBodySpec;

    ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/sendMessage")
    public SomeResponse sendMessage(@RequestBody SomeRequest someRequest) throws JsonProcessingException {
        log.debug("sendMessage, " + someRequest);
        String channel = "channelOne";

        Notification not = Notification.builder()
                .msg(someRequest.getMessage())
                .randNo(Integer.toString((int)(Math.random() * 10000 % 9999)))
                .build();
        String escapedJsonString = objectMapper.writeValueAsString(
                objectMapper.writeValueAsString(not));

        Map<String, Object> requestBody = new HashMap<>();
        String queryString = "mutation add {"
                + "    publish("
                + "        data:" + escapedJsonString + ","
                + "         name: \"" + someRequest.getChannel() + "\""
                + "    ){"
                + "        data"
                + "        name"
                + "    }"
                + "}";
        log.debug("queryString {}", queryString);
        requestBody.put("query", queryString);

        WebClient.ResponseSpec response = requestBodySpec
                .body(BodyInserters.fromValue(requestBody))
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                .acceptCharset(StandardCharsets.UTF_8)
                .retrieve();

        String bodyString = response.bodyToMono(String.class).block();

        log.debug("bodyString {}", bodyString);

        return SomeResponse.builder().response(someRequest.getMessage()).build();
    }
}

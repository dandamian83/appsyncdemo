package com.example.appsyncdemo.controller;

import com.example.appsyncdemo.aws.AppSyncUtil;
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

import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("main")
public class MainController {

    @Autowired
    AppSyncUtil appSyncUtil;

    @PostMapping("/sendMessage")
    public SomeResponse sendMessage(@RequestBody SomeRequest someRequest) throws JsonProcessingException {
        log.debug("sendMessage, " + someRequest);

        Notification not = Notification.builder()
                .msg(someRequest.getMessage())
                .randNo(Integer.toString((int)(Math.random() * 10000 % 9999)))
                .build();

        try {
            appSyncUtil.push(not, someRequest.getChannel());
        } catch (Exception e) {
            log.error("Exception", e);
            return SomeResponse.builder().response(e.getMessage()).build();
        }


        return SomeResponse.builder().response(someRequest.getMessage()).build();
    }
}

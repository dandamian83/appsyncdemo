package com.example.appsyncdemo.client;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SomeRequest {

    private String message;
    private String channel;
}

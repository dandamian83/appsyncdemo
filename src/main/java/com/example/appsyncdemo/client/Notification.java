package com.example.appsyncdemo.client;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Notification {

    private String msg;
    private String randNo;
}

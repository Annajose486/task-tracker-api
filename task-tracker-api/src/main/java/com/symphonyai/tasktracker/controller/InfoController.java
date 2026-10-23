package com.symphonyai.tasktracker.controller;

import com.symphonyai.tasktracker.dto.InfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/info")
public class InfoController {

    private final String appName;
    private final String appVersion;

    public InfoController(@Value("${app.name}") String appName,
                          @Value("${app.version}") String appVersion) {
        this.appName = appName;
        this.appVersion = appVersion;
    }

    @GetMapping
    public InfoResponse info() {
        return new InfoResponse(appName, appVersion);
    }
}

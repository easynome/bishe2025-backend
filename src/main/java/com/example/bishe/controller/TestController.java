package com.example.bishe.controller;

import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.service.UserCourseScoreService;
import com.example.bishe.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @Autowired
    private UserCourseScoreService userCourseScoreService;

    @GetMapping("/test")
    public UserCourseScore test() {
        return userCourseScoreService.findByUserIdAndCourseId(2L, 6L);
    }

}


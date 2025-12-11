package com.example.bishe;

import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.service.UserCourseScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserCourseScoreServiceTest {
    @Autowired
    private UserCourseScoreService userCourseScoreService;

    @Test
    void testFindByUserIdAndCourseId() {
        UserCourseScore score = userCourseScoreService.findByUserIdAndCourseId(1L, 1L);
        System.out.println("查询结果：" + score);
    }
}

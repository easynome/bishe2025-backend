package com.example.bishe.controller;

import com.example.bishe.entity.Course;
import com.example.bishe.entity.R;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.service.CourseService;
import com.example.bishe.service.UserCourseScoreService;
import com.example.bishe.common.BaseContext;
import com.example.bishe.util.JwtUtil;
import com.example.bishe.vo.MyCourseVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserCourseScoreController {
    private final UserCourseScoreService userCourseScoreService;
    private final CourseService courseService;

    //获取用户评分记录
    @GetMapping("/study/my-ratings")
    public R<List<Map<String, Object>>> getMyRatings(HttpServletRequest request){
        try {
            String token = request.getHeader("Authorization");
            token=token.substring(7);
            Long userId= JwtUtil.getUserIdFromToken(token);

            //查询用户的评分记录
           List<UserCourseScore> scores =userCourseScoreService.getScoresByUserId(userId);

           List<Map<String, Object>> result =new ArrayList<>();
           for(UserCourseScore score:scores){
               //查询课程信息
               Course course=courseService.getById(score.getCourseId());
               if(course!=null){
                   Map<String, Object> item =new HashMap<>();
                   item.put("courseId",course.getId());
                   item.put("courseName",course.getName());
                   item.put("credit",course.getCredit());
                   item.put("score",score.getScore());
                   item.put("createdAt",score.getCreatedAt());
                   result.add(item);
               }
           }
           return R.success(result);
        }catch (Exception e){
            e.printStackTrace();
            return R.failed("获取用户评分记录失败");
        }
    }

    @GetMapping("/study/my-courses")
    public R<List<MyCourseVO>> getMyCourses(){
        Long userId = BaseContext.getCurrentId();
        log.info("当前用户ID：{}",userId);
        List<MyCourseVO> list = userCourseScoreService.getMyCourseVOList(userId);
        return R.success(list);
    }
}

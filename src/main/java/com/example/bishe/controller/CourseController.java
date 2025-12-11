package com.example.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bishe.entity.Course;
import com.example.bishe.entity.R;
import com.example.bishe.entity.User;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.mapper.UserCourseScoreMapper;
import com.example.bishe.service.CourseService;
import com.example.bishe.service.UserService;
import com.example.bishe.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;
    private final UserCourseScoreMapper scoreMapper;
    private final UserService userService;
    private final UserCourseScoreMapper userCourseScoreMapper;

    //发布课程
    @PostMapping("/add")
    public R<String> addCourse(@RequestBody Course dto){
        courseService.save(dto);
        return R.success("发布成功");
    }
    //分页查询
    @GetMapping("/list")
    public R<Map<String, Object>> getCourseList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword){  //搜索关键字
        //创建查询条件
        LambdaQueryWrapper<Course> wrapper =new LambdaQueryWrapper<>();

        //模糊查询
        if(keyword!=null && !keyword.trim().isEmpty()){
            wrapper.like(Course::getName,keyword)
                    .or().like(Course::getDescription,keyword);
        }

        //分页查询
        Page< Course> pageInfo = new Page<>(page,pageSize);
        Page< Course> coursePage = courseService.page(pageInfo,wrapper);

        //返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("courses",coursePage.getRecords());
        result.put("total",coursePage.getTotal());
        result.put("page",coursePage.getPages());
        result.put("current",coursePage.getCurrent());
        result.put("size",coursePage.getSize());

        return R.success(result);
    }

    //获取所有课程
    @GetMapping("/all")
    public R<List<Course>> list(){
        return R.success(courseService.list());
    }

    //根据课程id获取课程
    @GetMapping("/{id}")
    public R<Course> get(@PathVariable Long id){
        return R.success(courseService.getById(id));
    }

    //获取课程学生
    @GetMapping("/{courseId}/students")
    public R<List<User>> students(@PathVariable Long courseId){
        //1.查询哪些userId选了这门课
        List<UserCourseScore> list =scoreMapper.selectList(
                Wrappers.<UserCourseScore>lambdaQuery()
                        .eq(UserCourseScore::getCourseId,courseId));
        //2.获取userId
        List<Long> userIds=list.stream()
                .map(UserCourseScore::getUserId)
                .collect(Collectors.toList());
        if(userIds.isEmpty()){
            return R.success(Collections.emptyList());
        }
        List<User> users=userService.listByIds(userIds);
        return R.success(users);
    }

    //{id}课程评分
    @PostMapping("/{id}/rate")
    public R<String> rateCourse(@PathVariable Long id,
                                @RequestParam Integer score,
                                HttpServletRequest  request) {
        try {
            //1.获取token
            String token = request.getHeader("Authorization");
            if(token== null|| !token.startsWith("Bearer ")){
                return R.failed("请先登录");
            }
            token=token.substring(7);

            //2.获取用户ID
            Long userId= JwtUtil.getUserIdFromToken(token);
            if(userId==null){
                return R.failed("用户ID获取失败");
            }
            //3.检查评分范围
            if(score <1|| score >5){
                return R.failed("评分须在范围1-5之间");
            }

            //4.检查用户是否已经评分
            UserCourseScore existing=userCourseScoreMapper.findByUserIdAndCourseId(userId,id);

            //5.更新或插入评分
            if(existing!=null){
                existing.setScore(score);
                userCourseScoreMapper.updateById(existing);
                return R.success("评分更新成功");
            }else {
                UserCourseScore newScore=new UserCourseScore();
                newScore.setUserId(userId);
                newScore.setCourseId(id);
                newScore.setScore(score);
                userCourseScoreMapper.insert(newScore);
                return R.success("评分成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return R.failed("评分失败"+e.getMessage());
        }
    }
}

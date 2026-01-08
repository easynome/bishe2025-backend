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
import com.example.bishe.service.impl.RecommendServiceImpl;
import com.example.bishe.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;
    private final UserCourseScoreMapper scoreMapper;
    private final UserService userService;
    private final UserCourseScoreMapper userCourseScoreMapper;
    private final RecommendServiceImpl recommendService;


    //分页查询
    @GetMapping("/list")
    public R<Map<String, Object>> getCourseList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            //搜索关键字
            @RequestParam(required = false) String keyword){

        //创建查询条件
        LambdaQueryWrapper<Course> wrapper =new LambdaQueryWrapper<>();

        //模糊查询
        if(keyword!=null && !keyword.trim().isEmpty()){
            wrapper.like(Course::getName,keyword)
                    .or().like(Course::getDescription,keyword);
        }

        //分页查询
        Page< Course> pageInfo = new Page<>(page,size);
        Page< Course> coursePage = courseService.page(pageInfo,wrapper);

        //返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("courses",coursePage.getRecords());
        result.put("total",coursePage.getTotal());
        result.put("pages",coursePage.getPages());
        result.put("current",coursePage.getCurrent());
        result.put("size",coursePage.getSize());

        return R.success(result);
    }

    /**教师端课程管理***/
    //发布课程
    @PostMapping("/teacher/add")
    public R<String> addCourse(@RequestBody Course course,
                               HttpServletRequest request){
        try{
            String token = request.getHeader("Authorization");
            if(token== null|| !token.startsWith("Bearer ")){
                return R.failed("请先登录");
            }
            token=token.substring(7);
            Long userId= JwtUtil.getUserIdFromToken(token);

            //防御性编程，数据安全 + 业务逻辑验证
            User user=userService.getById(userId);
            if(user==null|| user.getRoleId()!=2){
                return R.failed("您没有权限发布课程");
            }

            //设置教师ID
            course.setTeacherId(userId);

            //设置课程状态为已发布
            course.setStatus(1);

            if(course.getName()==null|| course.getName().trim().isEmpty()){
                return R.failed("课程名称不能为空");
            }
            if(course.getCredit()==null|| course.getCredit()<=0){
                return R.failed("课程学分必须大于0");
            }

            courseService.save(course);
            return R.success("发布成功");
        }catch (Exception e){
            e.printStackTrace();
            return R.failed("发布失败"+e.getMessage());
        }
    }

    //获取教师发布的课程
    @GetMapping("/teacher/my-courses")
    public R<Map<String, Object>> getMyCoursesForTeacher(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request){
        try {
            String token = request.getHeader("Authorization");
            token=token.substring(7);
            Long teacherId= JwtUtil.getUserIdFromToken(token);

            //验证用户角色
            User user=userService.getById(teacherId);
            if(user==null|| user.getRoleId()!=2){
                return R.failed("您没有权限查看课程");
            }

            //查询教师发布的课程
            LambdaQueryWrapper<Course> wrapper =new LambdaQueryWrapper<>();
            wrapper.eq(Course::getTeacherId,teacherId);

            //添加关键词搜索
            if(keyword != null && !keyword.trim().isEmpty()) {
//                wrapper.like(Course::getName, keyword)
//                        .or().like(Course::getDescription,keyword);
                wrapper.and(w->w
                        .like(Course::getName, keyword)
                        .or()
                        .like(Course::getDescription, keyword)
                );
            }

            //添加状态筛选
            if (status != null) {
                wrapper.eq(Course::getStatus, status);
            }

            //添加排序
            wrapper.orderByDesc(Course::getCreatedAt);

            //分页查询
            Page< Course> pageInfo = new Page<>(page,size);
            Page< Course> coursePage = courseService.page(pageInfo,wrapper);

            List<Map<String, Object>> courseList = coursePage.getRecords().stream()
                    .map(course -> {
                        Map<String, Object> item =new HashMap<>();
                        item.put("id",course.getId());
                        item.put("name",course.getName());
                        item.put("description",course.getDescription());
                        item.put("credit",course.getCredit());
                        item.put("status",course.getStatus());
                        item.put("teacherId",course.getTeacherId());
                        item.put("createdAt",course.getCreatedAt());

                        Long studentCount =userCourseScoreMapper.selectCount(
                                Wrappers.<UserCourseScore>lambdaQuery()
                                        .eq(UserCourseScore::getCourseId,course.getId())
                        );
                        item.put("studentCount",studentCount);
                        return item;
                    })
                    .collect(Collectors.toList());

            //返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("courses",courseList);
            result.put("total",coursePage.getTotal());
            result.put("pages",coursePage.getPages());
            result.put("current",coursePage.getCurrent());
            result.put("size",coursePage.getSize());
            return R.success(result);
        }catch (Exception e){
            e.printStackTrace();
            return R.failed("查询失败"+e.getMessage());
        }
    }

    //修改课程状态
    @PostMapping("/{id}/status")
    public R<String> updateCourseStatusByTeacher(@PathVariable Long id,
                                  @RequestParam Integer status,
                                  HttpServletRequest request
                                  ){
        try{
            String token = request.getHeader("Authorization");
            token=token.substring(7);
            Long teacherId= JwtUtil.getUserIdFromToken(token);

            Course course=courseService.getById(id);
            if(course==null){
                return R.failed("课程不存在");
            }
            if(course.getTeacherId() == null || !course.getTeacherId().equals(teacherId)){
                return R.failed("您没有权限更新课程");
            }
            //更新状态(0:下架 1:上架)
            if(status!=1&&status!=0){
                return R.failed("状态值无效");
            }
            course.setStatus(status);
            courseService.updateById(course);

            String statusText=status==1?"上架":"下架";
            return R.success("课程"+statusText+"成功");
        }catch (Exception e){
            e.printStackTrace();
            return R.failed("更新失败"+e.getMessage());
        }
    }

    /**管理员端接口***/

    //获取课程列表
    @GetMapping("/admin/courses/list")
    public R<Map<String, Object>> getCoursesForAdmin(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request){
        try {

            String token = request.getHeader("Authorization");
            token=token.substring(7);
            Long AdminId= JwtUtil.getUserIdFromToken(token);
            //验证用户角色
            User user=userService.getById(AdminId);
            if(user==null|| user.getRoleId()!=3){
                return R.failed("您没有权限查看");
            }
            LambdaQueryWrapper<Course> wrapper =new LambdaQueryWrapper<>();

            //添加关键词搜索
            if(keyword!= null && !keyword.trim().isEmpty()) {
                wrapper.and(w->w
                        .like(Course::getName, keyword)
                        .or()
                        .like(Course::getDescription, keyword)
                );
            }
            //添加状态筛选
            if (status != null) {
                wrapper.eq(Course::getStatus, status);
            }


            //按创建时间排序
            wrapper.orderByDesc(Course::getCreatedAt);

            // 2. 单独统计上架课程数量
            LambdaQueryWrapper<Course> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(Course::getStatus, 1);
            if(keyword != null && !keyword.trim().isEmpty()) {
                countWrapper.and(w->w
                        .like(Course::getName, keyword)
                        .or()
                        .like(Course::getDescription, keyword)
                );
            }
            long onShelvesCount = courseService.count(countWrapper);

            //分页查询
            Page< Course> pageInfo = new Page<>(page,size);
            Page< Course> coursePage = courseService.page(pageInfo,wrapper);

            //获取教师信息
            List<Course> courses = coursePage.getRecords();
            List<Map<String, Object>> courseList =new ArrayList<>();

            for (Course course : courses) {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", course.getId());
                courseMap.put("name", course.getName());
                courseMap.put("description", course.getDescription());
                courseMap.put("credit", course.getCredit());
                courseMap.put("status", course.getStatus());
                courseMap.put("createdAt", course.getCreatedAt());
                courseMap.put("teacherId", course.getTeacherId());


                //获取教师名称
                if (course.getTeacherId() != null) {
                    User teacher = userService.getById(course.getTeacherId());
                    if (teacher != null) {
                        courseMap.put("teacherName", teacher.getUsername());
                    }
                }
                //获取选课学生数
                Long studentCount = userCourseScoreMapper.selectCount(
                        Wrappers.<UserCourseScore>lambdaQuery()
                                .eq(UserCourseScore::getCourseId, course.getId())
                );
                courseMap.put("studentCount", studentCount);

                //获取平均分
                List<UserCourseScore> scores = userCourseScoreMapper.selectList(
                        Wrappers.<UserCourseScore>lambdaQuery()
                                .eq(UserCourseScore::getCourseId, course.getId())
                );
                if (!scores.isEmpty()) {
                    double avgSocre = scores.stream()
                            .mapToDouble(UserCourseScore::getScore)
                            .average()
                            .orElse(0.0);
                    courseMap.put("avgScore", Math.round(avgSocre * 10) / 10.0);
                } else {
                    courseMap.put("avgScore", 0.0);
                }
                courseList.add(courseMap);
            }
            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("courses", courseList);
            result.put("total", coursePage.getTotal());
            result.put("pages", coursePage.getPages());
            result.put("current", coursePage.getCurrent());
            result.put("size", coursePage.getSize());
            result.put("onShelvesCount", onShelvesCount);

            return R.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.failed("获取课程列表失败"+e.getMessage());
        }
    }

    //更新课程状态
    @PostMapping("/admin/courses/{id}/status")
    public R<String> updateCourseStatusByAdmin(@PathVariable Long id,
                                               @RequestParam Integer status,
                                               HttpServletRequest  request) {
        try {
            String token = request.getHeader("Authorization");
            token=token.substring(7);
            Long AdminId= JwtUtil.getUserIdFromToken(token);
            User user=userService.getById(AdminId);
            if(user==null|| user.getRoleId()!=3){
                return R.failed("您没有权限操作");
            }
            Course course=courseService.getById(id);
            if(course==null){
                return R.failed("课程不存在");
            }

            if(status!=1&&status!=0){
                return R.failed("状态值无效,0-下架，1-上架");
            }

            course.setStatus(status);
            courseService.updateById(course);

            String statusText=status==1?"上架":"下架";
            return R.success("课程"+statusText+"成功");

        } catch (Exception e) {
            e.printStackTrace();
            return R.failed("更新失败"+e.getMessage());
        }
    }

    //获取课程详情
    @GetMapping("/admin/courses/{id}")
    public R<Map<String, Object>> getCourseDetailsForAdmin(@PathVariable Long id) {
        try {
            Course course=courseService.getById(id);
            if(course==null){
                return R.failed("课程不存在");
            }
            Map<String, Object> result = new HashMap<>();
            result.put("id", course.getId());
            result.put("name", course.getName());
            result.put("description", course.getDescription());
            result.put("credit", course.getCredit());
            result.put("status", course.getStatus());
            result.put("createdAt", course.getCreatedAt());
            result.put("teacherId", course.getTeacherId());

            if(course.getTeacherId()!=null){
                User teacher=userService.getById(course.getTeacherId());
                if(teacher!=null){
                    result.put("teacherName", teacher.getUsername());
                    result.put("teacherCreatedAt", teacher.getCreatedAt());
                }
            }
            //获取学生信息
            List<UserCourseScore> scores = userCourseScoreMapper.selectList(
                    Wrappers.<UserCourseScore>lambdaQuery()
                            .eq(UserCourseScore::getCourseId, course.getId())
                            .orderByDesc(UserCourseScore::getCreatedAt)
            );

            //获取学生信息
            List<Map<String, Object>> studentList = new ArrayList<>();
            for (UserCourseScore score : scores) {
                User student = userService.getById(score.getUserId());
                if (student != null) {
                    Map<String, Object> studentMap = new HashMap<>();
                    studentMap.put("userId", student.getId());
                    studentMap.put("username", student.getUsername());
                    studentMap.put("score", score.getScore());
                    studentMap.put("rateAt", score.getCreatedAt());
                    studentList.add(studentMap);
                }
            }
            result.put("students", studentList);
            result.put("studentCount", scores.size());

            // 平均评分
            if (!scores.isEmpty()) {
                double avgScore = scores.stream()
                        .mapToInt(UserCourseScore::getScore)
                        .average()
                        .orElse(0.0);
                result.put("avgScore", Math.round(avgScore * 10) / 10.0);
            } else {
                result.put("avgScore", 0.0);
            }

            return R.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.failed("获取课程详情失败"+e.getMessage());
        }
    }

    //更新课程
    @PutMapping("/admin/courses/{id}")
    public R<String> updateCourseByAdmin(@PathVariable Long id,
                                         @RequestBody Course updateCourse,
                                         HttpServletRequest  request
                                ) {
        try {
            String token = request.getHeader("Authorization");
            token=token.substring(7);
            Long AdminId= JwtUtil.getUserIdFromToken(token);
            User user=userService.getById(AdminId);
            if(user==null|| user.getRoleId()!=3){
                return R.failed("您没有权限操作");
            }
            Course course = courseService.getById(id);
            if (course == null) {
                return R.failed("课程不存在");
            }

            if (updateCourse.getName() != null) {
                course.setName(updateCourse.getName());
            }
            if (updateCourse.getDescription() != null) {
                course.setDescription(updateCourse.getDescription());
            }
            if (updateCourse.getCredit() != null) {
                course.setCredit(updateCourse.getCredit());
            }
            courseService.updateById(course);
            return R.success("更新课程成功");

        } catch (Exception e) {
            e.printStackTrace();
            return R.failed("更新课程失败" + e.getMessage());
        }
    }

    //获取仪表盘数据
    @GetMapping("/data")
    public R<Map<String, Object>> getDashboardData() {
        Map<String, Object> result = new HashMap<>();
        List<UserCourseScore> allScores = userCourseScoreMapper.selectList(null);

        List<Course> hotList = recommendService.getHotCourse(allScores, 10);
        result.put("hotList", hotList);

        long studentCount = allScores.stream().map(UserCourseScore::getUserId).distinct().count();
        result.put("totalStudents", studentCount);

        result.put("courseCount", courseService.count());

        result.put("engineStatus","User-CF + Hot Fallback Enabled");
        result.put("updateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return R.success(result);
    }
    //获取所有课程
    @GetMapping("/all")
    public R<List<Course>> list(){
        return R.success(courseService.list());
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
    //根据课程id获取课程
    @GetMapping("/{id}")
    public R<Course> get(@PathVariable Long id){
        return R.success(courseService.getById(id));
    }

}

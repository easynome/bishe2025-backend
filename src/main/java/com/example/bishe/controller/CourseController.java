
package com.example.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bishe.common.BaseContext;
import com.example.bishe.common.annotation.Log;
import com.example.bishe.common.constants.RedisConstants;
import com.example.bishe.entity.Course;
import com.example.bishe.common.R;
import com.example.bishe.entity.User;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.mapper.UserCourseScoreMapper;
import com.example.bishe.service.CacheCleanService;
import com.example.bishe.service.CourseService;
import com.example.bishe.service.UserService;
import com.example.bishe.service.impl.RecommendServiceImpl;
import com.example.bishe.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程控制器 - 处理课程相关的API请求
 * 包含公共课程查询、教师端课程管理、管理员端课程管理等功能
 *
 * @author [作者名]
 * @date [创建日期]
 */
@Slf4j
@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
@Validated
public class CourseController {
    private final CourseService courseService;
    private final UserService userService;
    private final UserCourseScoreMapper userCourseScoreMapper;
    private final RecommendServiceImpl recommendService;
    private final CacheCleanService cacheCleanService;
    private final RedisUtil redisUtil;

    /**
     * 分页查询课程列表（公共接口，无需Token，拦截器已放行）
     *
     * @param page    页码，默认为1
     * @param size    每页大小，默认为10
     * @param keyword 搜索关键字，可选参数
     * @return 课程列表分页数据
     */
    @GetMapping("/list")
    public R<Map<String, Object>> getCourseList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {

        Page<Course> coursePage = courseService.getCoursePage(page, size, keyword);
        //构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("courses", coursePage.getRecords());
        result.put("total", coursePage.getTotal());
        result.put("pages", coursePage.getPages());
        result.put("current", coursePage.getCurrent());
        result.put("size", coursePage.getSize());

        return R.success(result);
    }

    /*
      教师端课程管理功能
     */

    /**
     * 发布新课程
     * 需要教师身份验证
     *
     * @param course  课程信息
     * @param request HTTP请求对象
     * @return 操作结果
     */
    @PostMapping("/teacher/add")
    public R<String> addCourse(@Valid @RequestBody Course course,
                               HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("currUserId");
        //防御性编程，数据安全 + 业务逻辑验证

        if (!userService.hasTeacherRole(userId)) {
            return R.failed("您没有权限发布课程");
        }
        //设置教师ID
        course.setTeacherId(userId);
        //设置课程状态为已发布
        course.setStatus(1);
        courseService.save(course);
        cacheCleanService.cleanCourseGlobalCache();
        return R.success("发布成功");
    }

    /**
     * 获取教师发布的课程列表
     * 需要教师身份验证
     *
     * @param page    页码，默认为1
     * @param size    每页大小，默认为10
     * @param keyword 搜索关键字，可选参数
     * @param status  课程状态筛选，可选参数
     * @param request HTTP请求对象
     * @return 教师课程列表分页数据
     */
    @GetMapping("/teacher/my-courses")
    public R<Map<String, Object>> getMyCoursesForTeacher(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {

        Long teacherId = (Long) request.getAttribute("currUserId");


        //验证用户角色
        if (!userService.hasTeacherRole(teacherId)) {
            return R.failed("您没有权限查看课程");
        }
        //查询教师发布的课程
        LambdaQueryWrapper<Course> wrapper = courseService.buildBaseQueryWrapper(keyword, status);
        wrapper.eq(Course::getTeacherId, teacherId);
        //添加排序
        wrapper.orderByDesc(Course::getCreatedAt);

        //分页查询
        Page<Course> pageInfo = new Page<>(page, size);
        Page<Course> coursePage = courseService.page(pageInfo, wrapper);

        List<Map<String, Object>> courseList = coursePage.getRecords().stream()
                .map(courseService::convertCourseToMap)
                .collect(Collectors.toList());

        //返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("courses", courseList);
        result.put("total", coursePage.getTotal());
        result.put("pages", coursePage.getPages());
        result.put("current", coursePage.getCurrent());
        result.put("size", coursePage.getSize());
        return R.success(result);
    }

    /**
     * 修改课程状态（教师端）
     * 需要教师身份验证
     *
     * @param id      课程ID
     * @param status  新的状态值
     * @param request HTTP请求对象
     * @return 操作结果
     */
    @PostMapping("/{id}/status")
    public R<String> updateCourseStatusByTeacher(
            @PathVariable Long id,
            @NotNull @Min(0) @Max(1) @RequestParam Integer status,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("currUserId");

        //查询+权限校验（异常由全局处理器捕获）
        courseService.getAndCheckTeacherAuth(id, teacherId);

        courseService.updateCourseStatus(id, status);
        //删除redis中缓存
        cacheCleanService.cleanDashboardCache();
        cacheCleanService.cleanCourseGlobalCache();
        return R.success("状态更新成功");
    }

    /*
      管理员端课程管理功能
     */

    /**
     * 获取课程列表（管理员端）
     * 需要管理员身份验证
     *
     * @param page    页码，默认为1
     * @param size    每页大小，默认为10
     * @param keyword 搜索关键字，可选参数
     * @param status  课程状态筛选，可选参数
     * @param request HTTP请求对象
     * @return 课程列表分页数据
     */
    @GetMapping("/admin/courses/list")
    public R<Map<String, Object>> getCoursesForAdmin(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("currUserId");

        //验证用户角色
        if (!userService.hasAdminRole(adminId)) {
            return R.failed("您没有权限查看");
        }
            
        LambdaQueryWrapper<Course> wrapper = courseService.buildBaseQueryWrapper(keyword, status);

        //按创建时间排序
        wrapper.orderByDesc(Course::getCreatedAt);
        // 单独统计上架课程数量
        long onShelvesCount = courseService.getOnShelvesCount(keyword);

        //分页查询
        Page<Course> pageInfo = new Page<>(page, size);
        Page<Course> coursePage = courseService.page(pageInfo, wrapper);

        //获取教师信息
        List<Course> courses = coursePage.getRecords();
        List<Map<String, Object>> courseList = courses.stream()
                .map(courseService::convertCourseWithTeacherInfo)
                .collect(Collectors.toList());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("courses", courseList);
        result.put("total", coursePage.getTotal());
        result.put("pages", coursePage.getPages());
        result.put("current", coursePage.getCurrent());
        result.put("size", coursePage.getSize());
        result.put("onShelvesCount", onShelvesCount);

        return R.success(result);
    }

    /**
     * 更新课程状态（管理员端）
     * 需要管理员身份验证
     *
     * @param id      课程ID
     * @param status  新的状态值
     * @param request HTTP请求对象
     * @return 操作结果
     */
    @PostMapping("/admin/courses/{id}/status")
    public R<String> updateCourseStatusByAdmin(
            @PathVariable Long id,
            @NotNull @Min(0) @Max(1) @RequestParam Integer status,
            HttpServletRequest request) {

        Long adminId = (Long) request.getAttribute("currUserId");
        courseService.getAndCheckAdminAuth(id, adminId);

        courseService.updateCourseStatus(id, status);
        // 清除Redis中的相关缓存
        cacheCleanService.cleanCourseGlobalCache();
        cacheCleanService.cleanDashboardCache();
        return R.success("状态更新成功");
    }

    /**
     * 获取课程详情（管理员端）
     * 需要管理员身份验证
     *
     * @param id 课程ID
     * @return 课程详细信息
     */
    @GetMapping("/admin/courses/{id}")
    public R<Map<String, Object>> getCourseDetailsForAdmin(@PathVariable Long id) {
        Course course = courseService.getById(id);
        if (course == null) {
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

        if (course.getTeacherId() != null) {
            User teacher = userService.getById(course.getTeacherId());
            if (teacher != null) {
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
        List<Map<String, Object>> studentList = scores.stream()
                .map(score -> {
                    User student = userService.getById(score.getUserId());
                    if (student != null) {
                        Map<String, Object> studentMap = new HashMap<>();
                        studentMap.put("userId", student.getId());
                        studentMap.put("username", student.getUsername());
                        studentMap.put("score", score.getScore());
                        studentMap.put("rateAt", score.getCreatedAt());
                        return studentMap;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

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
    }

    /**
     * 更新课程信息（管理员端）
     * 需要管理员身份验证
     *
     * @param id           课程ID
     * @param updateCourse 更新的课程信息
     * @param request      HTTP请求对象
     * @return 操作结果
     */
    @PutMapping("/admin/courses/{id}")
    public R<String> updateCourseByAdmin(
            @PathVariable Long id,
            @Valid @RequestBody Course updateCourse,
            HttpServletRequest request
    ) {
        // 从请求属性中获取当前管理员ID
        Long adminId = (Long) request.getAttribute("currUserId");
        // 验证管理员权限：检查指定ID的课程是否存在且当前用户具有管理员权限
        courseService.getAndCheckAdminAuth(id, adminId);

        courseService.updateCourseInfo(id, updateCourse);
        // 清除Redis中的相关缓存，确保数据一致性
        cacheCleanService.cleanCourseGlobalCache();  // 清除课程全局缓存
        cacheCleanService.cleanDashboardCache();     // 清除仪表盘缓存
        // 返回成功响应，提示课程更新完成
        return R.success("更新课程成功");
    }

    /**
     * 获取仪表盘统计数据
     * 使用Redis缓存提高访问性能
     *
     * @return 仪表盘数据
     */
    @GetMapping("/data")
//    @Cacheable(value = "dashboard", key = "'admin_stats'")
    public R<Map<String, Object>> getDashboardData() {
        String cacheKey = RedisConstants.DASHBOARD_STATS_KEY;
        //第一步：尝试从Redis缓存中获取数据（读取降级）
        try {
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                log.info("首页数据命中缓存");
                //注意：手动获取需要进行类型强转
                return R.success((Map<String, Object>) cached);
            }
        } catch (Exception e) {
            //如果Redis挂了，只记日志，程序继续往下走，实现降级
            log.error("Redis连接异常：首页开启自动降级查库模式：{}", e.getMessage());
        }
        //第二步：执行核心业务逻辑（从数据库中获取数据）
        log.info("首页数据未命中缓存，开始执行计算逻辑");
        Map<String, Object> result = new HashMap<>();

        try {
            //1，获取所有评分
            List<UserCourseScore> allScores = userCourseScoreMapper.selectList(
                    Wrappers.<UserCourseScore>lambdaQuery()
                            .eq(UserCourseScore::getCourseStatus, 1)
            );

            //2.计算热门榜单
            List<Course> hotList = recommendService.getHotCourse(allScores, 10);
            result.put("hotList", hotList);

            //3.获取总学生数(基于已发布课程的评分)
            long studentCount = allScores.stream()
                    .map(UserCourseScore::getUserId)
                    .distinct()
                    .count();
            result.put("totalStudents", studentCount);

            //4.获取课程数
            result.put("courseCount", courseService.count());

            //5.获取推荐引擎状态，补充元数据
            result.put("engineStatus", "User-CF + Hot Fallback Enabled");
            result.put("updateTime", LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } catch (Exception e) {
            log.error("真正的业务异常：{}", e.getMessage(), e);
            return R.failed("数据计算失败，请检查数据库连接");
        }
        //第三步：将结果写入Redis缓存（写入降级）
        try {
            //设置10分钟缓存+随机盐（防止雪崩）
            int expireTime = 600 + new Random().nextInt(300);
            redisUtil.set(cacheKey, result, expireTime);
        } catch (Exception e) {
            log.error("Redis写入失败：{}", e.getMessage());
        }
        return R.success(result);
    }

    /**
     * 对指定课程进行评分
     *
     * @param id      课程ID
     * @param score   评分值（1-5之间）
     * @return 操作结果
     */
    @Log(title = "课程评分", businessType = 2)
    @PostMapping("/{id}/rate")
    public R<String> rateCourse(
            @NotNull @PathVariable Long id,
            @NotNull
            @Min(value = 1, message = "评分须在范围1-5之间")
            @Max(value = 5, message = "评分须在范围1-5之间")
            @RequestParam Integer score
            ) {

        //基础校验
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return R.failed("用户ID获取失败");
        }
        courseService.savaOrUpdateScore(userId,id,score);

        cacheCleanService.cleanAllAfterAction(userId);
        return R.success("评分成功");
    }
    
    /**
     * 获取选修指定课程的学生列表
     *
     * @param courseId 课程ID
     * @return 学生列表
     */
    @GetMapping("/{courseId}/students")
    public R<List<User>> students(@PathVariable Long courseId) {
        //1.查询哪些userId选了这门课
        List<UserCourseScore> list = userCourseScoreMapper.selectList( // 使用统一的字段引用
                Wrappers.<UserCourseScore>lambdaQuery()
                        .eq(UserCourseScore::getCourseId, courseId));
        //2.获取userId
        List<Long> userIds = list.stream()
                .map(UserCourseScore::getUserId)
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return R.success(Collections.emptyList());
        }
        List<User> users = userService.listByIds(userIds);
        return R.success(users);
    }
    
    /**
     * 根据ID获取课程信息
     *
     * @param id 课程ID
     * @return 课程信息
     */
    @GetMapping("/{id}")
    public R<Course> get(@PathVariable Long id) {
        return R.success(courseService.getById(id));
    }
}

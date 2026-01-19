package com.example.bishe.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bishe.entity.Course;
import com.example.bishe.entity.User;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.mapper.CourseMapper;
import com.example.bishe.mapper.UserCourseScoreMapper;
import com.example.bishe.service.CacheCleanService;
import com.example.bishe.service.CourseService;
import com.example.bishe.service.UserService;
import com.example.bishe.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course>
        implements CourseService{
    private final RedisUtil redisUtil;
    private final UserService userService;
    private final CacheCleanService cacheCleanService;
    private final UserCourseScoreMapper userCourseScoreMapper;
    //默认继承list()实现
    @Override
    @Cacheable(value="course",key="'list_all'")
    public List<Course> list() {
        return super.list();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourseStatus(Long courseId, Integer newStatus){
        this.update(Wrappers.< Course>lambdaUpdate()
                .set(Course::getStatus,newStatus)
                .eq(Course::getId,courseId));

        //同步更新评分表冗余字段
        userCourseScoreMapper.update(Wrappers.<UserCourseScore>lambdaUpdate()
                .set(UserCourseScore::getCourseStatus,newStatus)
                .eq(UserCourseScore::getCourseId,courseId));
    }

    @Override
    public void updateCourseInfo(Long id, Course updateCourse) {
        Course course = this.getById(id);
        boolean statusChanged = false;
        if (updateCourse.getName() != null) {
            course.setName(updateCourse.getName());
        }
        if (updateCourse.getDescription() != null) {
            course.setDescription(updateCourse.getDescription());
        }
        if (updateCourse.getCredit() != null) {
            course.setCredit(updateCourse.getCredit());
        }
        //状态修改
        if (updateCourse.getStatus() != null &&
                !course.getStatus().equals(updateCourse.getStatus())) {
            course.setStatus(updateCourse.getStatus());
            this.updateCourseStatus(id, updateCourse.getStatus());
            statusChanged = true;

        } else {
            this.updateById(course);
        }
        if (statusChanged) {
            cacheCleanService.cleanDashboardCache();
        }
    }

    @Override
    public Course getById(Long id) {
        String key="course_"+id;
        try {
            //1.先查缓存
            Object cached = redisUtil.get(key);
            if (cached != null) {
                //如果缓存里是我们要的“空标记”，则直接返回null，不再查库
                if ("EMPTY_NODE".equals(cached)) return null;
                return (Course) cached;
            }
        }catch(Exception e){
            //如果Redis服务异常，则直接返回数据库查询结果，并记录错误日志，不抛出异常
            log.error("Redis服务异常，系统已自动降级为数据库查询，错误：{}",e.getMessage());
            //这里不返回，程序会继续往下走执行数据库查询
        }
        //2.查数据库
        Course course=super.getById(id);

        //3.尝试回写Redis
        try {
            if (course == null) {
                //如果数据库里没有，则将“空标记”存入缓存，并返回null
                redisUtil.set(key, "EMPTY_NODE", 120+new Random().nextInt(60));
                return null;
            }
            int expireTime = 3600 + new Random().nextInt(600);
            redisUtil.set(key, course, expireTime);
        }catch (Exception e){
            log.error("Redis写入失败，错误：{}",e.getMessage());
        }
        return course;
    }

    @Override
    @Cacheable(value="course",key="'page_' + #page + '_' + #size + '_' + (#keyword?:'')")
    public Page<Course> getCoursePage(Integer page, Integer size, String keyword) {
        //创建查询条件
        LambdaQueryWrapper<Course> wrapper =new LambdaQueryWrapper<>();
        //模糊查询
        if(keyword!=null && !keyword.trim().isEmpty()){
            wrapper.like(Course::getName,keyword)
                    .or().like(Course::getDescription,keyword);
        }
        //状态必须是已发布
        wrapper.eq(Course::getStatus,1);
        wrapper.orderByAsc(Course::getId);
        //分页查询
        Page< Course> pageInfo = new Page<>(page,size);
        return this.page(pageInfo,wrapper);

    }

    @Override
    public Course getAndCheckTeacherAuth(Long courseId, Long teacherId) {
        Course course = this.getById(courseId);
        if (course == null) throw new RuntimeException("课程不存在");
        if (!course.getTeacherId().equals(teacherId)) throw new RuntimeException("您没有权限访问该课程");
        return course;
    }

    @Override
    public Course getAndCheckAdminAuth(Long courseId, Long adminId) {
        Course course = this.getById(courseId);
        if (course == null) throw new RuntimeException("课程不存在");
        if (!userService.hasAdminRole(adminId))
            throw new RuntimeException("权限不足，仅管理员可操作");
        return course;
    }

    @Override
    public Long getOnShelvesCount(String keyword) {
        LambdaQueryWrapper<Course> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Course::getStatus, 1);
        if (keyword != null && !keyword.trim().isEmpty()) {
            countWrapper.and(w -> w
                    .like(Course::getName, keyword)
                    .or()
                    .like(Course::getDescription, keyword)
            );
        }
        return this.count(countWrapper);
    }

    @Override
    public LambdaQueryWrapper<Course> buildBaseQueryWrapper(String keyword, Integer status) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();

        //添加关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                    .like(Course::getName, keyword)
                    .or()
                    .like(Course::getDescription, keyword)
            );
        }
        //添加状态筛选
        if (status != null) {
            wrapper.eq(Course::getStatus, status);
        }

        return wrapper;
    }

    @Override
    public Map<String, Object> convertCourseWithTeacherInfo(Course course) {
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
            double avgScore = scores.stream()
                    .mapToDouble(UserCourseScore::getScore)
                    .average()
                    .orElse(0.0);
            courseMap.put("avgScore", Math.round(avgScore * 10) / 10.0);
        } else {
            courseMap.put("avgScore", 0.0);
        }
        return courseMap;
    }

    @Override
    public Map<String, Object> convertCourseToMap(Course course) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", course.getId());
        item.put("name", course.getName());
        item.put("description", course.getDescription());
        item.put("credit", course.getCredit());
        item.put("status", course.getStatus());
        item.put("teacherId", course.getTeacherId());
        item.put("createdAt", course.getCreatedAt());

        Long studentCount = userCourseScoreMapper.selectCount(
                Wrappers.<UserCourseScore>lambdaQuery()
                        .eq(UserCourseScore::getCourseId, course.getId())
        );
        item.put("studentCount", studentCount);
        return item;
    }

    //评分
    @Override
    public void savaOrUpdateScore(Long userId, Long id, Integer score) {
        //获取课程信息
        Course course = this.getById(id);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        //检查用户是否已经评分
        UserCourseScore existing = userCourseScoreMapper.findByUserIdAndCourseId(userId, id);
        //更新或插入评分
        if (existing != null) {
            existing.setScore(score);
            userCourseScoreMapper.updateById(existing);

        } else {
            UserCourseScore newScore = new UserCourseScore();
            newScore.setUserId(userId);
            newScore.setCourseId(id);
            newScore.setScore(score);
            newScore.setCourseStatus(course.getStatus());
            userCourseScoreMapper.insert(newScore);
        }
    }

}

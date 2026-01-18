package com.example.bishe.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bishe.entity.Course;
import com.example.bishe.entity.User;
import com.example.bishe.entity.UserCourseScore;
import com.example.bishe.mapper.UserCourseScoreMapper;
import com.example.bishe.service.UserCourseScoreService;
import com.example.bishe.vo.MyCourseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCourseScoreServiceImpl
        extends ServiceImpl<UserCourseScoreMapper, UserCourseScore>
        implements UserCourseScoreService {

    private final UserCourseScoreMapper userCourseScoreMapper;
    private final CourseServiceImpl courseService;
    private final UserServiceImpl userService;

    @Override
    public UserCourseScore findByUserIdAndCourseId(Long userId, Long courseId) {
        return userCourseScoreMapper.findByUserIdAndCourseId(userId, courseId);
    }

    public void saveScore(UserCourseScore score) {
        // 1. 获取课程当前状态
        Course course = courseService.getById(score.getCourseId());
        if (course != null) {
            // 2. 必须手动设置冗余字段的值
            score.setCourseStatus(course.getStatus());
        }
        this.save(score);
    }
    @Override
    @Cacheable(value="userCourseScore",key="#userId")
    public List<UserCourseScore> getScoresByUserId(Long userId) {
        return this.list(Wrappers.<UserCourseScore>lambdaQuery()
                .eq(UserCourseScore::getUserId,userId)
                .orderByDesc(UserCourseScore::getCreatedAt));
    }

    @Override
    public List<MyCourseVO> getMyCourseVOList(Long userId) {
        //1.查询该学生的所有选课记录
        List<UserCourseScore> scores = this.list(
                Wrappers.<UserCourseScore>lambdaQuery()
                        .eq(UserCourseScore::getUserId,userId)
                        .orderByDesc(UserCourseScore::getCreatedAt)
        );
        if(scores.isEmpty())return new ArrayList<>();

        //2.提取所有课程ID(courseIds),批量查询课程详情
        //技巧：使用Stream流提取ID集合，避免循环查库
        Set<Long> courseIds = scores.stream()
                .map(UserCourseScore::getCourseId)
                .collect(Collectors.toSet());

        //批量查出所有涉及的课程
        List<Course> courses = courseService.listByIds(courseIds);
        //为了方便后续缝合，转为Map<courseId,Course>
        Map<Long,Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getId,c->c));

        Set< Long> teacherIds= courses.stream()
                .map(Course::getTeacherId)
                .collect(Collectors.toSet());
        //批量查出所有老师的信息
        Map<Long,String> teacherNameMap =userService.listByIds(teacherIds).stream()
                .collect(Collectors.toMap(User::getId,User::getUsername));

        //4.最终缝合：将scores、courseMap、teacherNameMap组装成VO
        return scores.stream().map(score->{
            MyCourseVO vo = new MyCourseVO();
            Course c=courseMap.get(score.getCourseId());

            vo.setCourseId(c.getId());
            vo.setScore(score.getScore());
            vo.setCreatedAt(score.getCreatedAt());

            if(c!=null){
                vo.setCourseName(c.getName());
                vo.setCredit(c.getCredit());
                vo.setCourseStatus(c.getStatus());
                vo.setTeacherName(teacherNameMap.get(c.getTeacherId()));
            }
            return vo;
        }).collect(Collectors.toList());
    }
}

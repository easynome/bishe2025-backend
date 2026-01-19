package com.example.bishe.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.bishe.entity.Course;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface CourseService extends IService<Course> {
    //list()方法已经由父类IService提供
    Course getById(Long id);
    //定义查询分页方法
    Page<Course> getCoursePage(Integer page, Integer size, String keyword);
    void updateCourseStatus(Long courseId, Integer newStatus);

    void updateCourseInfo(Long id,Course updateCourse);
    Course getAndCheckTeacherAuth(Long courseId, Long teacherId);
    Course getAndCheckAdminAuth(Long courseId, Long adminId);

    Long getOnShelvesCount(String keyword);
    LambdaQueryWrapper<Course> buildBaseQueryWrapper(String keyword, Integer status);
    Map<String, Object> convertCourseWithTeacherInfo(Course course);
    Map<String, Object> convertCourseToMap(Course course);

    void savaOrUpdateScore(Long userId, Long id, Integer score);
}

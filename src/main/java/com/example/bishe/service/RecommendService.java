package com.example.bishe.service;

import com.example.bishe.entity.Course;
import com.example.bishe.entity.UserCourseScore;

import java.util.List;

public interface RecommendService {
    List<Course> recommend(Long userId,int topN);
    List<Course> getHotCourse(List<UserCourseScore> allScores, int topN);
}

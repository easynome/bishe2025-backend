package io.github.easynome.learnrecommend.service;

import io.github.easynome.learnrecommend.entity.Course;
import io.github.easynome.learnrecommend.entity.UserCourseScore;

import java.util.List;

public interface RecommendService {

    List<Course> recommend(Long userId,int topN);
    List<Course> getHotCourse(List<UserCourseScore> allScores, int topN);
    List<Course> recommendationCalculation(Long userId, int topN,String cacheKey);
}

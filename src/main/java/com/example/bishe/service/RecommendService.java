package com.example.bishe.service;

import com.example.bishe.entity.Course;
import java.util.List;

public interface RecommendService {
    List<Course> recommend(Long userId,int topN);
}

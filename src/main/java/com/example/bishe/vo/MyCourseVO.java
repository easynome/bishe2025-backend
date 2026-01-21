package com.example.bishe.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;


/**
     * vo类的作用：将数据库中的数据转换成前端需要的数据
 */
@Data
public class MyCourseVO {
    private Long courseId;
    private String courseName;
    private Integer credit;
    private String teacherName;//教师名：需要从User表缝合
    private Integer score;//我的评分，从UserCourseScore表获取
    private Integer courseStatus;
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public Date getCreatedAt() {
        return createdAt;
    }
}

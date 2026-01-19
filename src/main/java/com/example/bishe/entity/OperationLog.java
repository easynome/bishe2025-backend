package com.example.bishe.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor

/**
 * 1.注解功能
 * 建造者模式：为 OperationLog 类自动生成建造者模式的实现
 * 链式调用：支持 .field(value).field2(value2).build() 的链式设置方式
 * 灵活构建：可以按需设置任意字段来创建对象实例
 * 2. 使用场景
 * 对象创建：在需要记录操作日志时构建 OperationLog 实例
 * 字段赋值：支持灵活设置日志的各项属性（如标题、业务类型、操作IP等）
 * 代码简化：避免手动编写大量的 setter 方法调用
 */
@Builder
@TableName("sys_operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title; //模块名称
    private String businessType; //业务类型
    private String method; //方法名称
    private String requestMethod;   //请求方式
    private Long operatorId; //操作人员ID
    private String operUrl; //请求URL
    private String operIp; //操作IP
    private String operParam; //请求参数
    private String jsonResult; //返回参数
    private Integer status; //0正常 1异常
    private String errorMsg;//错误信息
    private Long costTime; //耗时
    @TableField(fill = FieldFill.INSERT)//自动填充创建时间
    private Date operTime;
}

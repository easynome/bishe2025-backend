package com.example.bishe.service.impl;

import com.example.bishe.entity.OperationLog;
import com.example.bishe.mapper.OperationLogMapper;
import com.example.bishe.service.AsyncLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AsyncLogServiceImpl  implements AsyncLogService {
    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 异步保存日志 -生产级实现
     */
    @Async("logExecutor") //真正生效的异步注解
    @Transactional(propagation = Propagation.REQUIRES_NEW)//确保新事务独立
    public void saveLog(OperationLog operLog) {
        try{
            //在存入数据库之前，对参数进行截断，避免数据过大
            if (operLog.getOperParam() != null && operLog.getOperParam().length() > 2000) {
                operLog.setOperParam(operLog.getOperParam().substring(0, 2000));
            }
            //1.存入数据库
            operationLogMapper.insert(operLog);
//            throw new RuntimeException("模拟异常");
            log.debug("线程{}成功记录日志：{}",Thread.currentThread().getName(),operLog.getTitle());

        }catch (Exception e){
            //2.异步任务的异常处理（关键）
            log.error("异步保存日志失败，正在执行Redis备用存储，错误信息：{}",e.getMessage());
            //3.降级策略：可以存到Redis、文件或发送警告
            try{
                redisTemplate.opsForList().rightPush("failed_logs",operLog);
            }catch (Exception ex){
                log.error("Redis备用存储失败，日志内容：{}",ex.getMessage());
            }

        }
    }
    //    /**
//     *降级存储：当数据库失败时
//     */
//    private void saveToFallbackStorage(OperationLog log, Exception e) {
//        try{
//            //方案1：存到Redis
//            redisTemplate.opsForList().leftPush("failed_logs",log);
//
//            //方案2：写入本地文件
//            String logDir = "D:/ideaTemps/failed_logs/";
//            Files.createDirectories(Paths.get(logDir));
//
//            String filename =logDir+"log_"+System.currentTimeMillis()+".json";
//            Files.write(Paths.get(filename),
//                    JsonUtil.toJson(log).getBytes(),
//                    StandardOpenOption.CREATE);
//
//            log.info("日志已降级存储到文件：{}",filename);
//        }catch (Exception ex){
//            //降级也失败了，也记录错误
//            log.error("日志降级存储失败，日志内容：{}",ex);
//        }
//    }
//    /**
//     * 有返回值的异步方法（可选）
//     */
//    @Async
//    public Future<Boolean> saveLogWithResult(OperationLog operLog) {
//        try {
//            operationLogMapper.insert(operLog);
//            return new AsyncResult<>(true);
//        } catch (Exception e) {
//            log.error("保存日志失败", e);
//            return new AsyncResult<>(false);
//        }
//    }
}

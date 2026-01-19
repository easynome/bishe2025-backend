package com.example.bishe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


import java.util.Arrays;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 日志异步线程池
     * @return
     */
    @Bean("logExecutor")
    public TaskExecutor logTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心配置
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);//线程池所使用的缓冲队列

        //其他重要配置
        executor.setKeepAliveSeconds(30);//非核心线程空闲30秒后回收
        executor.setThreadNamePrefix("log-async");//线程池前缀名

        //拒绝策略
        executor.setRejectedExecutionHandler(
                // 策略1：CallerRunsPolicy（推荐）：调用者线程执行
                //线程池满了 → 主线程自己干
                new ThreadPoolExecutor.CallerRunsPolicy()
                // 策略2. AbortPolicy（默认）：抛出异常
                // 线程池满了 → 抛RejectedExecutionException
                // 策略3. DiscardPolicy：直接丢弃
                // 线程池满了 → 默默丢弃任务
                // 4. DiscardOldestPolicy：丢弃最老任务
                // 线程池满了 → 丢弃队列最前面的任务
                // 对于日志场景，推荐CallerRunsPolicy：
                // 日志很重要，不能丢，线程池满了就主线程自己存
        );

        //线程池关闭时等待所有任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        //监控线程池状态
        if (log.isDebugEnabled()) {
            monitorThreadPool(executor);
        }
        return executor;
    }

    /**
     * 异步异常处理
     * @return
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return(throwable,method,params)->{
            log.error("异步任务执行失败，方法：{}，参数：{}：",
                    method.getName(), Arrays.toString(params),throwable);
        };
    }
    /**
     * 监控线程池状态
     * @param executor
     */
    private void monitorThreadPool(ThreadPoolTaskExecutor executor) {
        Thread monitorThread=new Thread(()->{
            while(!Thread.currentThread().isInterrupted()){
                try{
                    TimeUnit.SECONDS.sleep(30);
                    log.debug("线程池状态：核心={}，活跃={}，最大={}，队列={}/{},已完成任务数：{}",
                            executor.getCorePoolSize(),
                            executor.getActiveCount(),
                            executor.getMaxPoolSize(),
                            executor.getQueueSize(),
                            executor.getQueueCapacity(),
                            executor.getThreadPoolExecutor().getCompletedTaskCount());
                }catch (InterruptedException e){//线程被中断
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("thread-pool-monitor");
        monitorThread.start();
    }

}

package com.example.bishe;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TestService {
    @Async  // 使用默认的SimpleAsyncTaskExecutor
    public void testAsync() {
        System.out.println("线程名: " + Thread.currentThread().getName());
        // 输出：SimpleAsyncTaskExecutor-1, SimpleAsyncTaskExecutor-2, ...
    }
}
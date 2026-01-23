//package com.example.bishe;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@SpringBootTest
//class TestServiceTest {
//
//    @Autowired
//    private TestService testService;
//
//    @Test
//    void testThreads() throws InterruptedException {
//        for (int i = 0; i < 100; i++) {
//            testService.testAsync();
//        }
//
//        // 重要：主线程睡一会儿。
//        // 因为测试主线程一旦结束，JVM 就会关闭，可能看不到异步线程的完整输出
//        Thread.sleep(5000);
//    }
//}
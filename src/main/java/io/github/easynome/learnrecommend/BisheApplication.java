package io.github.easynome.learnrecommend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 个性化学习推荐系统启动入口。
 *
 * @author 吴景辉
 */
@EnableAsync
@SpringBootApplication
public class BisheApplication {

    public static void main(String[] args) {
        SpringApplication.run(BisheApplication.class, args);
    }

}

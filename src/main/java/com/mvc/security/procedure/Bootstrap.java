package com.mvc.security.procedure;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Bootstrap
 *
 * @author qiyichen
 * @create 2018/4/18 13:55
 *
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
@EnableSwagger2
public class Bootstrap {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Bootstrap.class).web(true).run(args);

    }
}

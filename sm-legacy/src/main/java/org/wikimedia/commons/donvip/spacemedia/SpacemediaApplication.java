package org.wikimedia.commons.donvip.spacemedia;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@EnableAsync
@EnableCaching
@EnableScheduling
@SpringBootApplication
@Import(SpacemediaConfiguration.class)
public class SpacemediaApplication implements SchedulingConfigurer {

    @Autowired
    private Executor taskExecutor;

    public static void main(String[] args) {
        SpringApplication.run(SpacemediaApplication.class, args);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor);
    }
}

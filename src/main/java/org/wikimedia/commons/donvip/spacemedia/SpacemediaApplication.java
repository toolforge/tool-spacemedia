package org.wikimedia.commons.donvip.spacemedia;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@EnableScheduling
@SpringBootApplication
public class SpacemediaApplication implements SchedulingConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(SpacemediaApplication.class, args);
	}

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
    }

    @Bean(destroyMethod="shutdown")
    public Executor taskExecutor() {
        // Allow to run NASA (3), ESA (1), SpaceX (1) updates in parallel
        return Executors.newScheduledThreadPool(5);
    }
}

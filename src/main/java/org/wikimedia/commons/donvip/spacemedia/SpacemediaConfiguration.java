package org.wikimedia.commons.donvip.spacemedia;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpacemediaConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService taskExecutor(@Value("${threads.number:8}") int threadsNumber) {
        return Executors.newScheduledThreadPool(threadsNumber);
    }
}

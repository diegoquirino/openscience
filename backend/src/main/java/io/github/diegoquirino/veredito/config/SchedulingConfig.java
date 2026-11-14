package io.github.diegoquirino.veredito.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} jobs, such as the hourly cleanup of codes and sessions. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {}

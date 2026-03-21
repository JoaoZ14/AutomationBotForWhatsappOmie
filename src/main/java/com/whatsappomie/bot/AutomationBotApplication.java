package com.whatsappomie.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutomationBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutomationBotApplication.class, args);
    }
}

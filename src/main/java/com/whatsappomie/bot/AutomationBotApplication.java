package com.whatsappomie.bot;

import com.whatsappomie.bot.config.DatabaseUrlEnvironmentSetup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutomationBotApplication {

    public static void main(String[] args) {
        DatabaseUrlEnvironmentSetup.apply();
        SpringApplication.run(AutomationBotApplication.class, args);
    }
}

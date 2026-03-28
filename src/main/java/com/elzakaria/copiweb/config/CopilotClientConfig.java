package com.elzakaria.copiweb.config;

import com.github.copilot.sdk.CopilotClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CopilotClientConfig {

    @Bean(destroyMethod = "close")
    public CopilotClient copilotClient() {
        return new CopilotClient();
    }
}

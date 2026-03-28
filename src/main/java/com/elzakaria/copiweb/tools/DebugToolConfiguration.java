package com.elzakaria.copiweb.tools;


import com.github.copilot.sdk.json.ToolDefinition;
import com.github.copilot.sdk.json.ToolInvocation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Configuration
public class DebugToolConfiguration {


    /// Return how many times slump is called
    private static CompletableFuture<Object> invoke(final ToolInvocation invocation) {
        final var slumpMessage = (String) invocation.getArguments().get("slumpMessage");
        final var slump = String.valueOf(StringUtils.countOccurrencesOf(slumpMessage, "slump"));
        return CompletableFuture.completedFuture(Map.of("slumpResult", String.join(";", "Slump ", slump)));
    }

    /// Slump slump tool to test the tool definition
    @Bean
    public ToolDefinition slumpSlump() {
        return ToolDefinition.create("slump", "Slump slump tool", Map.of(
                "type", "object",
                "properties", Map.of(
                        "slumpMessage", Map.of("message", "string")
                )
        ), DebugToolConfiguration::invoke);
    }
}

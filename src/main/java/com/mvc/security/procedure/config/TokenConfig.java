package com.mvc.security.procedure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "mvc")
@Component
public class TokenConfig {

    private Map<String, Map<String, String>> erc20 = new HashMap<>();
}

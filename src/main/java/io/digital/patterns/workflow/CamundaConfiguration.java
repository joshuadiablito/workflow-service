package io.digital.patterns.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.connect.plugin.impl.ConnectProcessEnginePlugin;
import org.camunda.spin.impl.json.jackson.format.JacksonJsonDataFormat;
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CamundaConfiguration {

    @Bean
    public ConnectProcessEnginePlugin connectProcessEnginePlugin() {
        return new ConnectProcessEnginePlugin();
    }

    @Bean
    public JacksonJsonDataFormat formatter(ObjectMapper objectMapper) {
        return new JacksonJsonDataFormat("application/json", objectMapper);
    }

    @Bean
    public ProcessEnginePlugin spinProcessEnginePlugin() {
        return new SpinProcessEnginePlugin();
    }

}

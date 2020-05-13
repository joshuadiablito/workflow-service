package io.digital.patterns.workflow.health;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@Slf4j
public class ReadinessController {

    private final ProcessEngineConfiguration processEngineConfiguration;

    public ReadinessController(ProcessEngineConfiguration processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @GetMapping(path = "/engine", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,String> readiness() {
        return Collections.singletonMap("engine", processEngineConfiguration.getProcessEngineName());
    }

}

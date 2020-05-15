package io.digital.patterns.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.digital.patterns.workflow.data.FormDataService;
import io.digital.patterns.workflow.data.FormDataVariablePersistListener;
import io.digital.patterns.workflow.data.FormObjectSplitter;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.history.handler.CompositeDbHistoryEventHandler;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.camunda.connect.plugin.impl.ConnectProcessEnginePlugin;
import org.camunda.spin.impl.json.jackson.format.JacksonJsonDataFormat;
import org.camunda.spin.plugin.impl.SpinProcessEnginePlugin;
import org.springframework.beans.factory.annotation.Value;
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


    @Configuration
    public static class S3VariablePersistenceConfiguration extends AbstractCamundaConfiguration {
         private final FormDataService formDataService;

        public S3VariablePersistenceConfiguration(FormDataService formDataService) {
            this.formDataService = formDataService;
        }


        @Override
        public void preInit(SpringProcessEngineConfiguration processEngineConfiguration) {
            processEngineConfiguration.setHistoryEventHandler(
                    new CompositeDbHistoryEventHandler(
                            new FormDataVariablePersistListener(
                                    formDataService,
                                    processEngineConfiguration.getRuntimeService(),
                                    processEngineConfiguration.getRepositoryService(),
                                    processEngineConfiguration.getHistoryService(),
                                    new FormObjectSplitter()
                            )));
            log.info("S3 variable persistence configured");
        }
    }
}

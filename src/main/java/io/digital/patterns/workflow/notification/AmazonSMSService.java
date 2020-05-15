package io.digital.patterns.workflow.notification;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
public class AmazonSMSService {

    public static String SMS_FAILURE = "SMS_FAILURE";

    private final AmazonSNSClient amazonSNSClient;
    private final RuntimeService runtimeService;

    public AmazonSMSService(AmazonSNSClient amazonSNSClient, RuntimeService runtimeService) {
        this.amazonSNSClient = amazonSNSClient;
        this.runtimeService = runtimeService;
    }

    public String sendSMS(String phoneNumber, String message, String executionId) {
        try {
            Assert.notNull(phoneNumber, "Phone number cannot be null");
            Assert.notNull(message, "Message cannot be null");
            Assert.notNull(executionId, "Execution id cannot be null");

            PublishResult result = amazonSNSClient.publish(new PublishRequest()
                    .withMessage(message)
                    .withPhoneNumber(phoneNumber)
            );
            log.info("SMS result '{}'", result.getMessageId());
            return result.getMessageId();
        } catch (Exception e) {
            log.error("Failed to send SMS", e);
            JSONObject object = new JSONObject();
            object.put("phoneNumber", phoneNumber);
            object.put("exception", e.getMessage());
            object.put("executionId", executionId);
            try {
                runtimeService.createIncident(SMS_FAILURE,
                        executionId, object.toString());
            } catch (Exception ex) {
                log.error("Failed to create incident '{}'", ex.getMessage());
            }
           throw new BpmnError(SMS_FAILURE, e.getMessage(), e);
        }
    }
}

package io.digital.patterns.workflow.pdf;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import io.digital.patterns.workflow.data.FormDataService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.spin.json.SpinJsonNode;
import org.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@Service
public class PdfService {

    private final AmazonS3 amazonS3;
    private final AmazonSimpleEmailService amazonSimpleEmailService;
    private final RuntimeService runtimeService;
    private final Environment environment;
    private final RestTemplate restTemplate;

    public PdfService(AmazonS3 amazonS3, AmazonSimpleEmailService amazonSimpleEmailService,
                      RuntimeService runtimeService, Environment environment, RestTemplate restTemplate) {
        this.amazonS3 = amazonS3;
        this.amazonSimpleEmailService = amazonSimpleEmailService;
        this.runtimeService = runtimeService;
        this.environment = environment;
        this.restTemplate = restTemplate;
    }


    public void requestPdfGeneration(@NotNull SpinJsonNode form,
                                     @NotNull SpinJsonNode formData,
                                     @NotNull String businessKey,
                                     @NotNull String executionId,
                                     String message) {
        requestPdfGeneration(
                form,
                businessKey,
                null,
                executionId,
                message,
                formData
        );

    }

    public void requestPdfGeneration(@NotNull SpinJsonNode form,
                                     @NotNull String businessKey,
                                     @NotNull String executionId) {
        requestPdfGeneration(form, businessKey, null, executionId);
    }


    public void requestPdfGeneration(@NotNull SpinJsonNode form,
                                     @NotNull String businessKey,
                                     String product,
                                     @NotNull String executionId) {
        requestPdfGeneration(form, businessKey, product, executionId, null, null);
    }

    public void requestPdfGeneration(@NotNull SpinJsonNode form,
                                     @NotNull String businessKey,
                                     String product,
                                     @NotNull String executionId,
                                     String callbackMessage,
                                     SpinJsonNode formData) {

        JSONObject formAsJson = new JSONObject(form.toString());

        String bucket = environment.getProperty("aws.form-data-bucket-name")
                + Optional.ofNullable(product).map( i -> "-" + i).orElse("");
        String formApiUrl = environment.getProperty("form-api.url");
        String formName = formAsJson.getString("name");

        String message = Optional.ofNullable(callbackMessage)
                .orElse(format("pdfGenerated_%s_%s", formName, formAsJson.getString("submissionDate")));
        String key = generateKey(formAsJson, businessKey);
        JSONObject payload = new JSONObject();

        try {
            if (formData == null) {
                S3Object object = amazonS3.getObject(bucket, key);
                String asJsonString = IOUtils.toString(object.getObjectContent(),
                        StandardCharsets.UTF_8.toString());
                payload.put("submission", new JSONObject().put("data", new JSONObject(asJsonString)));
            } else {
                payload.put("submission",  new JSONObject().put("data", new JSONObject(formData.toString())));
            }


            payload.put("webhookUrl", format("%s%s/api/process-instance/webhook/processInstance/%s/message/%s?variableName=%s",
                   environment.getProperty("server.servlet.context-path"),environment.getProperty("engine.webhook.url"),
                    executionId, message,
                    formName));
            payload.put("formUrl", format("%s/form/version/%s", formApiUrl, formAsJson.getString("formVersionId")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> body = new HttpEntity<>(payload.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    format("%s/pdf", formApiUrl),
                    HttpMethod.POST,
                    body,
                    String.class
            );

            log.info("PDF request submitted response status '{}'", response.getStatusCodeValue());
        } catch (Exception e) {
            log.error("Failed to send PDF", e);
            String configuration = new JSONObject(Map.of(
                    "formName", formName,
                    "dataKey", key,
                    "bucketName", bucket,
                    "exception", e.getMessage()

            )).toString();
            try {

                runtimeService.createIncident(
                        "FAILED_TO_REQUEST_PDF_GENERATION",
                        executionId,
                        configuration
                );
            } catch (Exception rex) {
                log.error("Failed to create incident {}", rex.getMessage());
            }
            throw new BpmnError("FAILED_TO_REQUEST_PDF_GENERATION",configuration, e);
        }

    }


    public void sendPDFs(String senderAddress, List<String> recipients, String body, String subject,
                         List<String> attachmentIds, String executionId) {


        if (recipients.isEmpty()) {
            log.warn("No recipients defined so not sending email");
            return;
        }

        List<String> filteredRecipients = recipients.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());

        try {

            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage mimeMessage = new MimeMessage(session);

            mimeMessage.setSubject(subject, "UTF-8");
            mimeMessage.setFrom(senderAddress);
            mimeMessage.setRecipients(Message.RecipientType.TO, filteredRecipients.stream()
            .map(recipient -> {
                Address address = null;
                try {
                    address = new InternetAddress(recipient);
                } catch (AddressException e) {
                    log.error("Failed to resolve to address {} {}", recipient, e.getMessage());
                }
                return address;
            }).toArray(Address[]::new));

            MimeMultipart mp = new MimeMultipart();
            BodyPart part = new MimeBodyPart();
            part.setContent(body, "text/html");
            mp.addBodyPart(part);

            attachmentIds.forEach(id -> {
                S3Object object = amazonS3.getObject(environment.getProperty("pdf.generator.aws.s3.pdf.bucketname"), id);
                try {
                    MimeBodyPart attachment = new MimeBodyPart();
                    DataSource dataSource = new ByteArrayDataSource(object.getObjectContent(), "application/pdf");
                    attachment.setDataHandler(new DataHandler(dataSource));
                    attachment.setFileName(id);
                    attachment.setHeader("Content-ID", "<" + UUID.randomUUID().toString() + ">");
                    mp.addBodyPart(attachment);
                } catch (IOException | MessagingException e) {
                    log.error("Failed to get data from S3 {}", e.getMessage());
                }
            });

            mimeMessage.setContent(mp);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(outputStream);
            RawMessage rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));
            SendRawEmailRequest sendEmailRequest = new SendRawEmailRequest(rawMessage);
            SendRawEmailResult result = amazonSimpleEmailService.sendRawEmail(sendEmailRequest);

            log.info("SES send result {}", result.getMessageId());
        } catch (Exception e) {
            try {
                runtimeService.createIncident(
                        "FAILED_TO_SEND_SES",
                        executionId,
                        new JSONObject(Map.of(
                                "exception", e.getMessage()
                        )).toString()
                );
            } catch (Exception rex) {
                log.error("Failed to create incident {}", rex.getMessage());
            }
            log.error("Failed to send SES", e);
            throw new BpmnError("FAILED_TO_SEND_SES", e.getMessage(), e);
        }

    }

    private String generateKey(JSONObject formAsJson, String businessKey) {
        String submittedBy = formAsJson.getString("submittedBy");
        String submissionDate = formAsJson.getString("submissionDate");
        String formName = formAsJson.getString("name");
        return FormDataService.key(businessKey, formName, submittedBy, submissionDate);
    }
}

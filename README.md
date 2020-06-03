# Workflow Service

![Service Builder API Test and Build](https://github.com/DigitalPatterns/workflow-service/workflows/Service%20Builder%20API%20Test%20and%20Build/badge.svg)


Integrated Camunda engine with Cockpit.

### Environment

#### Bootstrap configuration

The following environment variables are required to load properties from AWS secrets manager

* AWS_SECRETS_MANAGER_ENABLED
* AWS_REGION
* AWS_ACCESS_KEY
* AWS_SECRET_KEY
* SPRING_PROFILES_ACTIVE


#### Application configuration

The following properties need to be configured in AWS secrets manager

```json5
{
  "database.driver-class-name": "org.postgresql.Driver",
  "database.password": "",
  "database.username": "admin",
  "auth.url": "https://keycloak.example.com",
  "auth.clientId": "servicename",
  "auth.clientSecret": "secret",
  "auth.realm": "master",
  "aws.s3.formData": "bucketName",
  "aws.s3.pdfs": "bucketName2",
  "formApi.url": "https://formApi.example.com",
  "engine.webhook.url": "https://engine-service.example.com",
  "gov.notify.api.key": "xxxxxx",
  "database.url": "jdbc:postgresql://dbUrl.example.com:5432/engine?sslmode=require&currentSchema=public"
}
```

Example helm chart for install [helm - workflowservice](https://github.com/DigitalPatterns/helm/tree/master/workflowservice)

### Drone

Drone is only used for deployment as the Github Action workflow is used to push to quay. To deploy you will need to pass the commit tag

```
drone deploy --param WORKFLOW_SERVICE_TAG=sha UKHomeOffice/workflow-service <build number> <environment>
```


### Changing log levels

You can update the log levels at runtime by using the following POST command:

```js
http://localhost:8080/camunda/actuator/loggers/org.springframework.security
```

with a payload:

```json
{"configuredLevel": "DEBUG"}
```

***You will need a valid JWT token***

For more information please check [here](https://www.baeldung.com/spring-boot-changing-log-level-at-runtime)
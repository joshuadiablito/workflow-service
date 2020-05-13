# Workflow Service

Integrated Camunda engine with Cockpit.


#### Bootstrap configuration

The following environment variables are required to load properties from AWS secrets manager

* AWS_SECRETS_MANAGER_ENABLED
* AWS_REGION
* AWS_ACCESS_KEY
* AWS_SECRET_KEY
* SPRING_PROFILES_ACTIVE


#### Application configuration

The following properties need to be configured in AWS secrets manager

* database.driver-class-name
* database.password
* database.username
* auth.url
* auth.clientId
* auth.clientSecret
* auth.realm
* gov.notify.api.key

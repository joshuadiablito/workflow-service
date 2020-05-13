package io.digital.patterns.workflow.data

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectResult
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.history.HistoricProcessInstance
import spock.lang.Specification

class FormDataServiceSpec extends Specification {

    RuntimeService runtimeService = Mock()
    AmazonS3 amazonS3 = Mock()

    def service = new FormDataService(runtimeService, amazonS3)


    def 'can generate request'() {
        given: 'a form'
        def form = '''{
                            "submit": true,
                            "test": "apples",
                            "shiftDetailsContext" : {
                               "email": "email"
                            },
                            "form": {
                               "name": "testForm",
                               "formVersionId": "versionId",
                               "submittedBy": "test",
                               "submissionDate": "20200120T12:12:00",
                               "title": "test",
                               "process": {
                                  
                               }
                            }
                        
                      }'''
        and: 'process instance'
        HistoricProcessInstance processInstance = Mock()
        processInstance.getId() >> "processInstance"
        processInstance.getProcessDefinitionId() >> "processdefinitionid"
        processInstance.getBusinessKey() >> "businessKey"

        when: 'request is made'
        service.save(form, processInstance, "id", "test")


        then: 'request is not null'

        1 * amazonS3.putObject(_) >> {
            def request = it[0]
            assert request != null
            assert request.getMetadata() != null
            assert request.getMetadata().getUserMetaDataOf("processinstanceid") == 'processInstance'
            assert request.getMetadata().getUserMetaDataOf("processdefinitionid") == 'processdefinitionid'
            assert request.getMetadata().getUserMetaDataOf("formversionid") == 'versionId'
            assert request.getMetadata().getUserMetaDataOf("name") == 'testForm'
            assert request.getMetadata().getUserMetaDataOf("submittedby") == 'email'
            assert request.getMetadata().getUserMetaDataOf("submissiondate") == '20200120T12:12:00'
            assert request.getMetadata().getUserMetaDataOf("title") == 'test'
            assert request.getKey().contains('businessKey/testForm/email')
            def result = new PutObjectResult()
            result.setETag("etag")
            return result
        }
    }

}

package io.digital.patterns.workflow

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import spock.lang.Specification

@SpringBootTest
class WorkflowServiceApplicationSpec extends Specification {

    @Autowired
    ApplicationContext applicationContext

    def 'can start up context'(){
        expect: 'application context not to be null'
        applicationContext
    }
}

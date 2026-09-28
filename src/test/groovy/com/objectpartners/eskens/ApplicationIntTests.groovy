package com.objectpartners.eskens

import com.objectpartners.eskens.controllers.PersonController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles('test')
class ApplicationIntTests extends Specification {

    @Autowired PersonController personController

    void 'context loads'() {
        expect:
        personController
    }
}

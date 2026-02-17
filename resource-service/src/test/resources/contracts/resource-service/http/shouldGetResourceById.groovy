package contracts.resource_service.http

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should get resource binary data by id"
    request {
        method GET()
        url "/resources/1"
    }
    response {
        status OK()
        headers {
            header('Content-Type', 'audio/mpeg')
        }
    }
}

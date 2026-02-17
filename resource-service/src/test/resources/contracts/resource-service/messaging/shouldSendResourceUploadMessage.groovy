package contracts.resource_service.messaging

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should send resource upload message to RabbitMQ when resource is uploaded"
    label "resource_uploaded"
    input {
        triggeredBy("triggerResourceUploadMessage()")
    }
    outputMessage {
        sentTo "resource.exchange"
        headers {
            header("contentType", "application/json")
            header("amqp_receivedRoutingKey", "resource.upload.routing.key")
        }
        body(
            resourceId: 1L,
            storageBucket: $(consumer(anyNonEmptyString()), producer("resource-bucket")),
            storageKey: $(consumer(anyNonEmptyString()), producer("resources/1"))
        )
    }
}

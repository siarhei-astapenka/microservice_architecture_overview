package contracts.song_service.http

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should save song metadata and return id"
    request {
        method POST()
        url "/songs"
        headers {
            contentType(applicationJson())
        }
        body(
            id: 1L,
            name: "Test Song",
            artist: "Test Artist",
            album: "Test Album",
            duration: "03:45",
            year: "2023"
        )
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
            id: 1L
        )
    }
}

package test.aws.task.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;
import test.aws.task.TaskException;
import test.aws.task.service.DataService;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class DataController {

    @Value("${user.message-length}")
    private int messageLengthLimit;

    @Value("${user.userId-length}")
    private int userIdLengthLimit;

    private final DataService dataService;

    @PostMapping(value = "/set/{userId}/{id}")
    public Mono<ResponseEntity<Boolean>> setUserId(@PathVariable final String userId, @PathVariable final String id,
                                                   @RequestBody final String data) throws TaskException {
        validateSize(userId, id);
        return dataService.setData(userId, id, data)
                .map(success -> ResponseEntity.ok(success))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(500).body(false)));
    }

    @GetMapping(value = "/get/{userId}/{id}")
    public Mono<ResponseEntity<String>> getUserId(@PathVariable final String userId, @PathVariable final String id) throws TaskException {
        validateSize(userId, id);
        return dataService.getData(userId, id)
                .map(data -> ResponseEntity.ok(data))
                .switchIfEmpty(Mono.error(new Exception("No data for these ids")))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(500).body(error.getMessage())));
    }

    private void validateSize(final String userId, final String id) throws TaskException {
        if (userId.length() > userIdLengthLimit) {
            throw new TaskException("Validation failed > 256");
        }
        if (id.length() > messageLengthLimit) {
            throw new TaskException("Validation failed > 256");
        }

    }
}

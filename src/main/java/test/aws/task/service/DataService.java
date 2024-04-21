package test.aws.task.service;

import reactor.core.publisher.Mono;

public interface DataService {

    Mono<Boolean> setData(String userId, String id, String data);

    Mono<String> getData(String userId, String id);
}
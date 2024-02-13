## Rate limit 구현하기

Leaky bucket algorithm 으로 간단한 요청 속도 제한을 구현한다.

<img width="985" alt="image" src="https://github.com/ecsimsw/java-rate-limit/assets/46060746/e2937b57-53cb-4e31-b033-9c8010c94845">

## 기능
- 처리 주기 (rate) : 요청이 처리되는 간격
- 대기열 (burst) : 처리 간격보다 이른 요청을 임시 큐에 저장한다. 
- 에러 응답 : 대기열이 가득찬 상태에서의 추가 요청은 버려진다. (429 Too many requests 응답) 
- 즉시 처리 (noDelay) : 대기열에 도착한 요청을 대기없이 즉시 처리한다. 단, 대기열에는 남기고 fixedFlow 는 유지하여 전체 처리 개수 제한은 유지한다.
- 공유 큐 : WAS 간 큐 공유를 위해 Redis 를 사용한다. (단일 WAS의 경우 메모리 사용)

## 미리 보기

핸들러의 요청 처리 속도를 제한한다. 아래는 rate = 1r/100ms, burst = 5 인 경우의 예시이다.     
10 개의 요청을 동시에 전송했을 때 5개만 처리되고 나머지는 429 (Too Many Requests) 를 응답받는 것을 확인할 수 있다. 그리고 처리되는 5개의 요청은 속도 제한에 따라 0.1초에 하나씩 처리된다.

```
HTTP/1.1 429     0.00 secs:      17 bytes ==> GET  /foo
HTTP/1.1 429     0.00 secs:      17 bytes ==> GET  /foo
HTTP/1.1 429     0.00 secs:      17 bytes ==> GET  /foo
HTTP/1.1 429     0.00 secs:      17 bytes ==> GET  /foo
HTTP/1.1 429     0.00 secs:      17 bytes ==> GET  /foo
HTTP/1.1 200     0.01 secs:       2 bytes ==> GET  /foo
HTTP/1.1 200     0.11 secs:       2 bytes ==> GET  /foo
HTTP/1.1 200     0.21 secs:       2 bytes ==> GET  /foo
HTTP/1.1 200     0.31 secs:       2 bytes ==> GET  /foo
HTTP/1.1 200     0.41 secs:       2 bytes ==> GET  /foo
```

nodelay 가 true 인 경우에는 5개의 요청을 처리하는 것은 동일하지만 (처리 수 제한), 즉시 처리되어 응답 지연이 없는 것을 확인할 수 있다.
```
HTTP/1.1 200     0.01 secs:       2 bytes ==> GET  /foo
HTTP/1.1 200     0.01 secs:       2 bytes ==> GET  /foo
HTTP/1.1 200     0.01 secs:       2 bytes ==> GET  /foo
HTTP/1.1 200     0.01 secs:       2 bytes ==> GET  /foo
HTTP/1.1 200     0.01 secs:       2 bytes ==> GET  /foo
HTTP/1.1 429     0.01 secs:      17 bytes ==> GET  /foo
HTTP/1.1 429     0.01 secs:      17 bytes ==> GET  /foo
HTTP/1.1 429     0.02 secs:      17 bytes ==> GET  /foo
HTTP/1.1 429     0.02 secs:      17 bytes ==> GET  /foo
HTTP/1.1 429     0.02 secs:      17 bytes ==> GET  /foo
```

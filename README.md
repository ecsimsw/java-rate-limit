## Rate limit 구현하기

Leaky bucket algorithm 으로 간단한 요청 속도 제한을 구현한다.

## 미리 보기

핸들러의 요청 처리 속도를 제한한다. 아래 핸들러는 0.1초에 한 번으로 처리 속도가 제한되고, 5개까지 보관해 두었다가 속도에 맞춰 처리한다.

``` java
@RestController
public class MainController {

    @RateLimit(rate = 100, burst = 5, noDelay = false)
    @RequestMapping("/foo")
    ResponseEntity<String> handleFoo() {
        return ResponseEntity.ok("hi");
    }
}
```

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

Transactions:		          10 hits
Availability:		      100.00 %
Elapsed time:		        0.41 secs
Data transferred:	        0.00 MB
Response time:		        0.11 secs
Transaction rate:	       24.39 trans/sec
Throughput:		        0.00 MB/sec
Concurrency:		        2.56
Successful transactions:           5
Failed transactions:	           0
Longest transaction:	        0.41
Shortest transaction:	        0.00
```

## 제어 조건

- rate : ms 단위로 대기열에서 요청이 처리되는 간격을 표시한다.
- burst : 대기열 크기를 표시한다. 대기열이 가득 찬 상태에서 추가 요청은 429(Too many requests) 로 응답된다.
- noDelay : 대기열에 도착한 요청을 대기없이 즉시 처리한다. 단, 대기열에는 남기고 fixedFlow 는 유지하여 전체 처리 흐름은 유지한다.   

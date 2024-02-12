package ecsimsw.ratelimit;

import java.util.concurrent.TimeoutException;

public interface LeakyBucket <T> {


    void put(T id);

    void putAndWait(T id) throws TimeoutException;
}

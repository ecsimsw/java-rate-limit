package ecsimsw.ratelimit;

import java.util.concurrent.TimeoutException;

public interface LeakyBucket {

    void put(int id);

    void putAndWait(int id) throws TimeoutException;

    void fixedFlow();
}

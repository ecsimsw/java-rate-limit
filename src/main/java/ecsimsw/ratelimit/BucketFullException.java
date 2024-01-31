package ecsimsw.ratelimit;

public class BucketFullException extends IllegalArgumentException {

    public BucketFullException(String msg) {
        super(msg);
    }
}

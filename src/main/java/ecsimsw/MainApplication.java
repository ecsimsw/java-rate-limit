package ecsimsw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MainApplication {

    public static void main(String[] args) {
        var app = new SpringApplication(MainApplication.class);
        app.setAdditionalProfiles("standalone");
        app.run();
    }
}

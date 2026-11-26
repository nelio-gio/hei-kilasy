package hei.school;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class KilasyApplication {

    public static void main(String[] args) {
        SpringApplication.run(KilasyApplication.class, args);
    }

    /**
     * Temporary SMS provider: the message is written to the logs.
     * To use another provider, replace this bean.
     */
    @Bean
    public SmsSender smsSender() {
        return (phoneNumber, message) -> System.out.println("[SMS] " + phoneNumber + " : " + message);
    }
}

interface SmsSender {

    void send(String phoneNumber, String message);
}

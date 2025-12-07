package com.online_games_service.tests;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public GreetingResponse hello(@RequestParam(defaultValue = "World") String name) {
        String message = "Hello " + name;
        return new GreetingResponse(message);
    }

    // JSON: {"message":"Hello Kuba","length":10}
    public static class GreetingResponse {
        private String message;

        public GreetingResponse() {
        }

        public GreetingResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public int getLength() {
            return message != null ? message.length() : 0;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

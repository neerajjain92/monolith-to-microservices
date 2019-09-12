package com.neeraj.hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class HelloApplication {

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable String name) {
        return "HELLO From Application :  " + name;
    }

    public static void main(String[] args) {
        SpringApplication.run(HelloApplication.class, args);
    }

}

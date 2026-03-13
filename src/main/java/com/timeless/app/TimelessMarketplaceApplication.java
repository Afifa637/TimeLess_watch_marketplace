package com.timeless.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TimelessMarketplaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimelessMarketplaceApplication.class, args);
    }
}


//docker compose down -v
//docker compose build --no-cache
//docker compose up
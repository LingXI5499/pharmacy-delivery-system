
package com.pharmacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PharmacyDeliveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(PharmacyDeliveryApplication.class, args);
    }
}

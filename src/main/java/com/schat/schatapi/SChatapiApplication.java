package com.schat.schatapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.schat.schatapi",
    "com.schat.signature-module "
})
public class SChatapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SChatapiApplication.class, args);
	}

}

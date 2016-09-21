package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;

@Configuration
@SpringBootApplication
@EnableIntegration
public class AcceptOnceFileListFilterIssueApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcceptOnceFileListFilterIssueApplication.class, args);
	}
}

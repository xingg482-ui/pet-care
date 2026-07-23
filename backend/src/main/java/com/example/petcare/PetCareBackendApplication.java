package com.example.petcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@MapperScan("com.example.petcare")
@SpringBootApplication
public class PetCareBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetCareBackendApplication.class, args);
	}

}

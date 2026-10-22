package com.UBM.BMW;

import org.springframework.boot.SpringApplication;

public class TestUbmApplication {

	public static void main(String[] args) {
		SpringApplication.from(UbmApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

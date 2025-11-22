package com.example.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UiServiceApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(UiServiceApplication.class, args);
    }
}

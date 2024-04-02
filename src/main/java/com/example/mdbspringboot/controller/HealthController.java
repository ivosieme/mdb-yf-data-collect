package com.example.mdbspringboot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // Aggregate root
    // tag::get-aggregate-root[]
    @GetMapping("/")
    String all() {
        return "Status - OK";
    }
    // end::get-aggregate-root[]
}

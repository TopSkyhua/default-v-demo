package com.lth.json.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * <p>
 * test
 * </p>
 *
 * @author Tophua
 * @since 2021/8/14
 */
@RestController
@AllArgsConstructor
@RequestMapping("/test")
public class TestController {

    private final ObjectMapper mapper;

    @SneakyThrows
    @GetMapping("/json")
    public Test test() {
        Test test = new Test();
        test.setTest1(new Test.Test1());
        test.setNow(LocalDateTime.now());
        System.out.println("mapper.writeValueAsString(test) = " + mapper.writeValueAsString(test));
        return test;
    }
}

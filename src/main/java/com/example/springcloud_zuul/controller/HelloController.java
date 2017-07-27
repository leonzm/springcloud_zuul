package com.example.springcloud_zuul.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: Leon
 * @CreateDate: 2017/7/27
 * @Description:
 * @Version: 1.0.0
 */
@RestController
public class HelloController {

    @RequestMapping(value = "/local/hello", method = RequestMethod.GET)
    public String hello() {
        return "Hello world local";
    }

}

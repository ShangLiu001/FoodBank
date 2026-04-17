package com.primaryfeed.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {"/dashboard", "/inventory", "/operations", "/community", "/reports", "/login"})
    public String forward() {
        return "forward:/index.html";
    }
}

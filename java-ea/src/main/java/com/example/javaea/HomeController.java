package com.example.javaea;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/soap")
    public String soap(){
        return "soap";
    }

    @GetMapping("/account")
    public String account(){
        return "account";
    }
    @GetMapping("/actual")
    public String actual(){
        return "actual";
    }
    @GetMapping("/historical")
    public String historical(){
        return "historical";
    }
    @GetMapping("/open")
    public String open(){
        return "open";
    }
    @GetMapping("/positions")
    public String positions(){
        return "positions";
    }
    @GetMapping("/close")
    public String close(){
        return "close";
    }


}
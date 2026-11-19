package com.example.javaea;

import com.example.javaea.service.OandaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final OandaService oandaService;

    public HomeController(OandaService oandaService) {
        this.oandaService = oandaService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/soap")
    public String soap() {
        return "soap";
    }

    @GetMapping("/account")
    public String account(Model model) {
        model.addAttribute("oandaConfigured", oandaService.isConfigured());
        try {
            model.addAttribute("account", oandaService.getAccountSummary());
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "account";
    }

    @GetMapping("/actual")
    public String actual(Model model) {
        model.addAttribute("oandaConfigured", oandaService.isConfigured());
        model.addAttribute("instruments", OandaService.INSTRUMENTS);
        model.addAttribute("instrument", "EUR_USD");
        return "actual";
    }

    @PostMapping("/actual")
    public String actualQuery(@RequestParam String instrument, Model model) {
        model.addAttribute("oandaConfigured", oandaService.isConfigured());
        model.addAttribute("instruments", OandaService.INSTRUMENTS);
        model.addAttribute("instrument", instrument);
        try {
            model.addAttribute("price", oandaService.getCurrentPrice(instrument));
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "actual";
    }


}
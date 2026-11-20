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
    @GetMapping("/historical")
    public String historical(Model model) {
        model.addAttribute("oandaConfigured", oandaService.isConfigured());
        model.addAttribute("instruments", OandaService.INSTRUMENTS);
        model.addAttribute("granularities", OandaService.GRANULARITIES);
        model.addAttribute("instrument", "EUR_USD");
        model.addAttribute("granularity", "D");
        model.addAttribute("candles", Collections.emptyList());
        return "historical";
    }

    @PostMapping("/historical")
    public String historicalQuery(
            @RequestParam String instrument,
            @RequestParam String granularity,
            Model model) {
        model.addAttribute("oandaConfigured", oandaService.isConfigured());
        model.addAttribute("instruments", OandaService.INSTRUMENTS);
        model.addAttribute("granularities", OandaService.GRANULARITIES);
        model.addAttribute("instrument", instrument);
        model.addAttribute("granularity", granularity);
        try {
            List<Map<String, Object>> candles = oandaService.getHistoricalPrices(instrument, granularity);
            model.addAttribute("candles", candles);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("candles", Collections.emptyList());
        }
        return "historical";
    }
    @GetMapping("/open")
    public String open(Model model) {
        model.addAttribute("oandaConfigured", oandaService.isConfigured());
        model.addAttribute("instruments", OandaService.INSTRUMENTS);
        model.addAttribute("instrument", "EUR_USD");
        model.addAttribute("units", 100);
        return "open";
    }

    @PostMapping("/open")
    public String openTrade(
            @RequestParam String instrument,
            @RequestParam BigDecimal units,
            Model model) {
        model.addAttribute("oandaConfigured", oandaService.isConfigured());
        model.addAttribute("instruments", OandaService.INSTRUMENTS);
        model.addAttribute("instrument", instrument);
        model.addAttribute("units", units);
        try {
            if (units.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("A mennyiség nem lehet 0.");
            }
            model.addAttribute("result", oandaService.openTrade(instrument, units));
            model.addAttribute("success", "Pozíció megnyitva piaci áron.");
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "open";
    }

    @GetMapping("/positions")
    public String positions(Model model) {
        model.addAttribute("oandaConfigured", oandaService.isConfigured());
        try {
            model.addAttribute("trades", oandaService.getOpenTrades());
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("trades", Collections.emptyList());
        }
        return "positions";
    }
    @GetMapping("/close")
    public String close(Model model) {
        model.addAttribute("oandaConfigured", oandaService.isConfigured());
        model.addAttribute("tradeId", "");
        loadOpenTradesForClose(model);
        return "close";
    }
    @PostMapping("/close")
    public String closeTrade(@RequestParam String tradeId, Model model) {
        model.addAttribute("oandaConfigured", oandaService.isConfigured());
        model.addAttribute("tradeId", tradeId);
        try {
            model.addAttribute("result", oandaService.closeTrade(tradeId));
            model.addAttribute("success", "Pozíció lezárva piaci áron.");
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("TRADE_DOESNT_EXIST")) {
                model.addAttribute("error",
                        "Ez a trade ID nem létezik (már le van zárva, vagy hibás szám). Nézd meg a Forex-Poz listát, és a táblázatból másold be az ID-t.");
            } else {
                model.addAttribute("error", msg);
            }
        }
        loadOpenTradesForClose(model);
        return "close";
    }

    private void loadOpenTradesForClose(Model model) {
        try {
            model.addAttribute("trades", oandaService.getOpenTrades());
        } catch (Exception e) {
            model.addAttribute("trades", Collections.emptyList());
        }
    }
}
package com.example.javaea;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import soapclient.MNBArfolyamServiceSoap;
import soapclient.MNBArfolyamServiceSoapImpl;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class SoapController {

    private final List<String> currencies = List.of("EUR", "USD", "GBP", "CHF", "JPY", "CZK", "PLN", "RON");

    @GetMapping("/soap")
    public String showSoapForm(Model model) {
        model.addAttribute("currencies", currencies);
        model.addAttribute("selectedCurrency", "EUR");
        model.addAttribute("startDate", LocalDate.now().minusDays(14).toString());
        model.addAttribute("endDate", LocalDate.now().toString());
        return "soap";
    }

    @PostMapping("/soap")
    public String getExchangeRates(
            @RequestParam("currency") String currency,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            Model model) {

        model.addAttribute("currencies", currencies);
        model.addAttribute("selectedCurrency", currency);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        try {
            MNBArfolyamServiceSoapImpl impl = new MNBArfolyamServiceSoapImpl();
            MNBArfolyamServiceSoap service = impl.getCustomBindingMNBArfolyamServiceSoap();
            String xmlResult = service.getExchangeRates(startDate, endDate, currency);

            List<String> dates = new ArrayList<>();
            List<Double> rates = new ArrayList<>();

            if (xmlResult != null && !xmlResult.isBlank()) {
                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(new InputSource(new StringReader(xmlResult)));

                NodeList dayNodes = doc.getElementsByTagName("Day");
                for (int i = 0; i < dayNodes.getLength(); i++) {
                    Element dayElem = (Element) dayNodes.item(i);
                    String date = dayElem.getAttribute("date");
                    Element rateElem = (Element) dayElem.getElementsByTagName("Rate").item(0);
                    if (rateElem != null) {
                        double unit = rateElem.hasAttribute("unit")
                                ? Double.parseDouble(rateElem.getAttribute("unit"))
                                : 1.0;
                        double rawRate = Double.parseDouble(rateElem.getTextContent().trim().replace(",", "."));
                        double rateValue = Math.round((rawRate / unit) * 10000.0) / 10000.0;

                        dates.add(date);
                        rates.add(rateValue);
                    }
                }
            }

            Collections.reverse(dates);
            Collections.reverse(rates);

            model.addAttribute("dates", dates);
            model.addAttribute("rates", rates);
            model.addAttribute("rawXml", xmlResult);

        } catch (Exception e) {
            model.addAttribute("error", "Hiba történt az MNB lekérdezés során: " + e.getMessage());
        }

        return "soap";
    }
}
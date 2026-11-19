package com.example.javaea.service;

import com.example.javaea.config.OandaProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OandaService {
    public static final List<String> INSTRUMENTS = List.of(
            "EUR_USD", "GBP_USD", "USD_JPY", "USD_CHF", "AUD_USD",
            "USD_CAD", "NZD_USD", "EUR_GBP", "EUR_HUF", "USD_HUF"
    );

    public static final List<String> GRANULARITIES = List.of(
            "S5", "M1", "M5", "M15", "H1", "H4", "D", "W"
    );

    private final OandaProperties props;
    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public OandaService(OandaProperties props, RestClient.Builder builder, JsonMapper jsonMapper) {
        this.props = props;
        this.jsonMapper = jsonMapper;
        this.restClient = builder
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getToken())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public boolean isConfigured() {
        return props.getToken() != null
                && !props.getToken().isBlank()
                && !props.getToken().startsWith("IDE_IRD")
                && props.getAccountId() != null
                && !props.getAccountId().isBlank()
                && !props.getAccountId().startsWith("IDE_IRD");
    }

}
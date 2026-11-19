package com.example.javaea.service;

import com.example.javaea.config.OandaProperties;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;

    public OandaService(OandaProperties props, JsonMapper jsonMapper) {
        this.props = props;
        this.jsonMapper = jsonMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
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

    public Map<String, Object> getAccountSummary() {
        ensureConfigured();
        JsonNode root = get(apiUri("/v3/accounts/" + props.getAccountId() + "/summary"));
        JsonNode account = root.path("account");
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", text(account, "id"));
        map.put("alias", text(account, "alias"));
        map.put("currency", text(account, "currency"));
        map.put("balance", text(account, "balance"));
        map.put("nav", text(account, "NAV"));
        map.put("unrealizedPL", text(account, "unrealizedPL"));
        map.put("marginUsed", text(account, "marginUsed"));
        map.put("marginAvailable", text(account, "marginAvailable"));
        map.put("openTradeCount", text(account, "openTradeCount"));
        map.put("openPositionCount", text(account, "openPositionCount"));
        return map;
    }

    public Map<String, Object> getCurrentPrice(String instrument) {
        ensureConfigured();
        String q = URLEncoder.encode(instrument, StandardCharsets.UTF_8);
        JsonNode root = get(apiUri("/v3/accounts/" + props.getAccountId() + "/pricing?instruments=" + q));
        JsonNode price = root.path("prices").path(0);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("instrument", text(price, "instrument"));
        map.put("time", text(price, "time"));
        map.put("bid", firstPrice(price.path("bids")));
        map.put("ask", firstPrice(price.path("asks")));
        map.put("status", text(price, "status"));
        map.put("tradeable", price.path("tradeable").asBoolean(false));
        return map;
    }

    public List<Map<String, Object>> getHistoricalPrices(String instrument, String granularity) {
        ensureConfigured();
        String path = "/v3/instruments/" + instrument
                + "/candles?granularity=" + URLEncoder.encode(granularity, StandardCharsets.UTF_8)
                + "&count=10&price=M";
        JsonNode root = get(apiUri(path));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonNode candle : root.path("candles")) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("time", text(candle, "time"));
            row.put("complete", candle.path("complete").asBoolean(false));
            JsonNode mid = candle.path("mid");
            row.put("open", text(mid, "o"));
            row.put("high", text(mid, "h"));
            row.put("low", text(mid, "l"));
            row.put("close", text(mid, "c"));
            row.put("volume", candle.path("volume").asString(""));
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> openTrade(String instrument, BigDecimal units) {
        ensureConfigured();
        String payload = """
                {"order":{"instrument":"%s","units":"%s","type":"MARKET","timeInForce":"FOK","positionFill":"DEFAULT"}}
                """.formatted(instrument, units.toPlainString());
        JsonNode root = sendJson("POST", apiUri("/v3/accounts/" + props.getAccountId() + "/orders"), payload);
        Map<String, Object> map = new LinkedHashMap<>();
        JsonNode fill = root.path("orderFillTransaction");
        if (!fill.isMissingNode()) {
            map.put("tradeId", text(fill.path("tradeOpened"), "tradeID"));
            map.put("instrument", text(fill, "instrument"));
            map.put("units", text(fill, "units"));
            map.put("price", text(fill, "price"));
            map.put("pl", text(fill, "pl"));
            map.put("time", text(fill, "time"));
            map.put("type", text(fill, "type"));
        } else {
            map.put("raw", root.toString());
        }
        return map;
    }

    public List<Map<String, Object>> getOpenTrades() {
        ensureConfigured();
        JsonNode root = get(apiUri("/v3/accounts/" + props.getAccountId() + "/openTrades"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonNode trade : root.path("trades")) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", text(trade, "id"));
            row.put("instrument", text(trade, "instrument"));
            row.put("currentUnits", text(trade, "currentUnits"));
            row.put("price", text(trade, "price"));
            row.put("unrealizedPL", text(trade, "unrealizedPL"));
            row.put("openTime", text(trade, "openTime"));
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> closeTrade(String tradeId) {
        ensureConfigured();
        JsonNode root = sendJson(
                "PUT",
                apiUri("/v3/accounts/" + props.getAccountId() + "/trades/" + tradeId + "/close"),
                "{\"units\":\"ALL\"}");
        Map<String, Object> map = new LinkedHashMap<>();
        JsonNode tx = root.path("orderFillTransaction");
        if (tx.isMissingNode()) {
            tx = root.path("orderCreateTransaction");
        }
        map.put("tradeId", tradeId);
        map.put("instrument", text(tx, "instrument"));
        map.put("units", text(tx, "units"));
        map.put("price", text(tx, "price"));
        map.put("pl", text(tx, "pl"));
        map.put("time", text(tx, "time"));
        map.put("type", text(tx, "type"));
        return map;
    }

    private URI apiUri(String pathAndQuery) {
        String base = props.getBaseUrl() == null ? "" : props.getBaseUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!pathAndQuery.startsWith("/")) {
            pathAndQuery = "/" + pathAndQuery;
        }
        return URI.create(base + pathAndQuery);
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Oanda API nincs beállítva. Állítsd be az oanda.api.token és oanda.api.account-id értékeket az application.properties fájlban.");
        }
    }

    private JsonNode get(URI uri) {
        return send(HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + props.getToken())
                .header("Accept", "application/json")
                .GET()
                .build(), uri);
    }

    private JsonNode sendJson(String method, URI uri, String json) {
        return send(HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + props.getToken())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(json))
                .build(), uri);
    }

    private JsonNode send(HttpRequest request, URI uri) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body();
            int status = response.statusCode();
            if (status >= 400) {
                throw new IllegalStateException("Oanda API hiba (" + status + ") @ " + uri + ": " + body);
            }
            String trimmed = body.stripLeading();
            if (trimmed.startsWith("<")) {
                throw new IllegalStateException(
                        "Oanda HTML-t adott vissza JSON helyett @ " + uri
                                + " (HTTP " + status + "). Válasz eleje: "
                                + trimmed.substring(0, Math.min(180, trimmed.length())));
            }
            return jsonMapper.readTree(body);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Oanda API hívás sikertelen @ " + uri + ": " + e.getMessage(), e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asString();
    }

    private static String firstPrice(JsonNode array) {
        if (array.isArray() && !array.isEmpty()) {
            return array.get(0).path("price").asString("");
        }
        return "";
    }
}

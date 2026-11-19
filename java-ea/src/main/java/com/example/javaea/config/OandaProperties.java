package com.example.javaea.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oanda.api")

public class OandaProperties {
    private String baseUrl = "http://api-fxpractice.oanda.com";
    private String token= "";
    private String accountId = "";

    public String getBaseUrl(){
        return baseUrl;
    }

    public  void setBaseUrl(String baseUrl){
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}

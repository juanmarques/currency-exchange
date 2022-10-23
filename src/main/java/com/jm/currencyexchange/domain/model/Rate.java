package com.jm.currencyexchange.domain.model;

public record Rate(String fromCurrencyCode, String fromCurrencyName, String toCurrencyCode, String toCurrencyName,
                   double exchangeRate) {
}

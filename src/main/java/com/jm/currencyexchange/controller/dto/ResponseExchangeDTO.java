package com.jm.currencyexchange.controller.dto;

public record ResponseExchangeDTO(String currencyCode, String Country, double amount, String exchangePath,
                                  double exchangeRate) {
}

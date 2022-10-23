package com.jm.currencyexchange.service;

import com.jm.currencyexchange.controller.dto.RequestExchangeDTO;
import com.jm.currencyexchange.controller.dto.ResponseExchangeDTO;
import com.jm.currencyexchange.domain.model.Rate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface IRateService {

    /**
     * @return a list with Currency Rates
     */
    List<Rate> getRates();

    /**
     * Find the best exchange rates using Dijkstra's algorithm
     *
     * @param requestRateDTO The request body with from the initial currencyCode exchange to desired currencyCode exchange and amount
     */
    List<ResponseExchangeDTO> findTheBestRates(RequestExchangeDTO requestRateDTO);

    /**
     * Build a Traverse Graphs with given rates
     *
     * @param rates list of currencyCode rates
     */
    ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> buildGraph(List<Rate> rates);

    /**
     * Builds the exchange path
     *
     * @param parent The parent edge
     * @param from   the initial currencyCode exchange
     * @param to     desired currencyCode exchange
     * @return The best exchange rate path
     */
    String buildExchangePath(Map<String, String> parent, String from, String to);

    /**
     * Generate the Excel output file with the best exchange rates for the given currency
     *
     * @param responseExchangeDTOList The best exchange rates for the given currency code
     * @param fromCurrencyCode        the initial currency code
     */
    void generateExcel(List<ResponseExchangeDTO> responseExchangeDTOList, String fromCurrencyCode);
}

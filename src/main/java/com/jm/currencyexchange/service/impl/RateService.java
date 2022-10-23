package com.jm.currencyexchange.service.impl;

import com.jm.currencyexchange.controller.dto.RequestExchangeDTO;
import com.jm.currencyexchange.controller.dto.ResponseExchangeDTO;
import com.jm.currencyexchange.controller.exception.InternalServerError;
import com.jm.currencyexchange.domain.model.Edge;
import com.jm.currencyexchange.domain.model.Rate;
import com.jm.currencyexchange.service.IRateService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateService implements IRateService {

    Logger log = LogManager.getLogger(RateService.class);

    private final RestTemplate restTemplate;

    public RateService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetch rates from given API
     *
     * @return List of fetched rates
     */
    @Override
    public List<Rate> getRates() {

        log.info("Fetching rates");

        var ratesResponse = restTemplate
                .getForEntity("https://api-coding-challenge.neofinancial.com/currency-conversion?seed={id}", Rate[].class, 59518);

        if (ratesResponse.getStatusCode().is2xxSuccessful() && ratesResponse.hasBody()) {
            log.info("done");
            return Arrays.asList(ratesResponse.getBody());
        }

        throw new InternalServerError("Failed to fetch rates");
    }

    /**
     * @param requestRateDTO The request body with from the initial currencyCode exchange to desired currencyCode exchange and amount
     * @return list
     */
    @Override
    public List<ResponseExchangeDTO> findTheBestRates(RequestExchangeDTO requestRateDTO) {

        var from = requestRateDTO.fromCurrencyCode();

        var rates = getRates();

        var graph = buildGraph(rates);

        var queue = new PriorityQueue<Edge>();

        // Initialize the queue with initial currency from code and placeholder exchange rate
        queue.add(new Edge(null, requestRateDTO.fromCurrencyCode(), 1.0));

        var neighbors = new HashMap<String, Double>();

        //Map to build exchange path string
        var parent = new HashMap<String, String>();

        parent.put(from, from);

        //visit all nodes until there are no more nodes to extract from the priority queue. Then, we return the calculated distances.
        while (!queue.isEmpty()) {

            Edge edge = queue.poll();
            var top = edge.currencyCode();

            if (neighbors.containsKey(top) && neighbors.get(top) < edge.exchangeRate()) {
                continue;
            }

            //update its neighbors’ distances
            neighbors.put(top, edge.exchangeRate());
            parent.put(top, edge.parent());
            graph.get(from).put(top, edge.exchangeRate());

            for (var dest : graph.get(top).entrySet()) {
                var rate = graph.get(from).get(top) * dest.getValue();

                if (neighbors.containsKey(dest.getKey()) && neighbors.get(dest.getKey()) >= rate || dest.getKey().equals(from)) {
                    continue;
                }

                // Pushing node to priority queue
                queue.add(new Edge(top, dest.getKey(), rate));
            }

        }

        // Build response DTO used to build the Excel file and return message
        var responseExchangeDTOList = graph.get(from).keySet().parallelStream()
                //Filter placeholder from added before
                .filter(key -> !key.equalsIgnoreCase(from))
                // Create new ResponseDTO
                .map(toKey -> {
                    // Find extra data needed to build the response dto using the rate lst
                    var optionalRate = rates.parallelStream().filter(r -> r.toCurrencyCode().equalsIgnoreCase(toKey)).findFirst();
                    if (optionalRate.isPresent()) {
                        var exchangeData = optionalRate.get();
                        var exchangeRate = graph.get(from).get(toKey);
                        return new ResponseExchangeDTO(
                                exchangeData.toCurrencyCode(),
                                exchangeData.toCurrencyName(),
                                requestRateDTO.amount() * exchangeRate,
                                buildExchangePath(parent, from, toKey),
                                graph.get(from).get(toKey));
                    }
                    return new ResponseExchangeDTO("", "", 1, "", 1);
                })
                .filter(item -> !item.currencyCode().isEmpty())
                .toList();

        generateExcel(responseExchangeDTOList, from);

        return responseExchangeDTOList;
    }

    /**
     * Build a Traverse Graphs with given rates
     *
     * @param rates list of currencyCode rates
     */
    @Override
    public ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> buildGraph(List<Rate> rates) {

        ConcurrentHashMap<String, ConcurrentHashMap<String, Double>> graph = new ConcurrentHashMap<>();

        rates.parallelStream().forEach(rate -> {
            if (!graph.containsKey(rate.fromCurrencyCode())) {
                graph.put(rate.fromCurrencyCode(), new ConcurrentHashMap<>());
            }
            graph.get(rate.fromCurrencyCode()).put(rate.toCurrencyCode(), rate.exchangeRate());
        });

        rates.parallelStream().forEach(rate -> graph.get(rate.fromCurrencyCode())
                .entrySet().parallelStream()
                .forEach(fromCurrency -> {

                    // If graph doesn't contain the key already we create an entry for the currencyCode
                    if (!graph.containsKey(fromCurrency.getKey())) {
                        graph.put(fromCurrency.getKey(), new ConcurrentHashMap<>());
                    }

                    graph.get(fromCurrency.getKey()).put(fromCurrency.getKey(), 1.0);

                    if (!graph.get(fromCurrency.getKey()).containsKey(rate.fromCurrencyCode())) {

                        graph.get(fromCurrency.getKey())
                                .put(rate.fromCurrencyCode(), 1.0 / fromCurrency.getValue());
                    }
                }));

        return graph;
    }

    /**
     * @param parent The parent edge
     * @param from   the initial currencyCode exchange
     * @param to     desired currencyCode exchange
     * @return the exchange path taken
     */
    @Override
    public String buildExchangePath(Map<String, String> parent, String from, String to) {
        String current = to;
        var res = new Stack<String>();
        res.add(to);
        while (!parent.get(current).equals(from)) {
            current = parent.get(current);
            res.add(current);
        }

        StringBuilder stringBuilder = new StringBuilder();
        res.add(from);

        while (!res.isEmpty()) {

            stringBuilder.append(res.pop());
            if (!res.isEmpty()) {
                stringBuilder.append(" | ");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Generate the Excel output file with the best exchange rates for the given currency
     *
     * @param responseExchangeDTOList The best exchange rates for the given currency code
     * @param fromCurrencyCode        the initial currency code
     */
    @Override
    public void generateExcel(List<ResponseExchangeDTO> responseExchangeDTOList, String fromCurrencyCode) {
        String[] headers = {"Currency Code", "Country", "Amount of currency", "Exchange Path", "Exchange Rate"};

        try {
            FileWriter out = new FileWriter("best_rates_for_" + fromCurrencyCode + ".csv");
            try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
                    .withHeader(headers))) {
                responseExchangeDTOList.forEach(item -> {
                    try {
                        printer.printRecord(item.currencyCode(), item.Country(), item.amount(), item.exchangePath(), item.exchangeRate());
                    } catch (IOException iox) {
                        log.error("Failed to export excel data", iox);
                    }
                });
            }
        } catch (IOException iox) {
            log.error("Failed to create excel file", iox);
        }

    }

    /**
     * Create and populate cells for each row
     *
     * @param exchangeDTO the list with best rates for given exchange code
     * @param row         new row for each item
     */
    void populateExcelFileRows(ResponseExchangeDTO exchangeDTO, Row row) {

        Cell cell = row.createCell(0);
        cell.setCellValue(exchangeDTO.currencyCode());

        cell = row.createCell(1);
        cell.setCellValue(exchangeDTO.Country());

        cell = row.createCell(2);
        cell.setCellValue(exchangeDTO.amount());

        cell = row.createCell(3);
        cell.setCellValue(exchangeDTO.exchangePath());

        cell = row.createCell(4);
        cell.setCellValue(exchangeDTO.exchangeRate());

    }
}
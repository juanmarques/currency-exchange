package com.jm.currencyexchange.controller;

import com.jm.currencyexchange.controller.dto.RequestExchangeDTO;
import com.jm.currencyexchange.controller.dto.ResponseExchangeDTO;
import com.jm.currencyexchange.service.IRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("exchange")
public class RateServiceController {

    Logger log = LogManager.getLogger(RateServiceController.class);

    private final IRateService rateService;

    public RateServiceController(IRateService rateService) {
        this.rateService = rateService;
    }

    @PostMapping
    @Operation(summary = "Find the best exchange rates")
    @ApiResponse(responseCode = "200", description = "Exchange rates successfully retrieved", content = {
            @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = RequestExchangeDTO.class)))
    })
    @ApiResponse(responseCode = "500", description = "Failed to fetch rates")
    public ResponseEntity<List<ResponseExchangeDTO>> findTheBestRate(@RequestBody RequestExchangeDTO requestRateDTO) {

        log.info("Received request {}", requestRateDTO);

        var bestRates = rateService
                .findTheBestRates(requestRateDTO);

        log.info("Best exchange rates {} ", bestRates);

        return ResponseEntity.ok(bestRates);
    }
}

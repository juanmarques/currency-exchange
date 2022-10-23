package com.jm.currencyexchange.service.impl;

import com.jm.currencyexchange.domain.model.Rate;
import com.jm.currencyexchange.service.IFindRate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FindRateService implements IFindRate {

    @Override
    public List<Rate> getRates() {
        return null;
    }

    @Override
    public void findBestRate(String from, String to) {

    }
}

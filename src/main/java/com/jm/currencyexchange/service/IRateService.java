package com.jm.currencyexchange.service;

import com.jm.currencyexchange.domain.model.Rate;

import java.util.List;

public interface IFindRate {

    List<Rate> getRates();

    void findBestRate(String from, String to);
}

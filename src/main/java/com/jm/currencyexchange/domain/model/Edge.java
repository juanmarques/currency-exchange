package com.jm.currencyexchange.domain.model;

import java.util.Objects;

public record Edge(String parent, String currencyCode, double exchangeRate) implements Comparable<Edge> {

    @Override
    public int compareTo(Edge o) {
        if (Objects.equals(this.exchangeRate, o.exchangeRate)) {
            return 1;
        } else {
            return Double.compare(this.exchangeRate, o.exchangeRate);
        }
    }
}

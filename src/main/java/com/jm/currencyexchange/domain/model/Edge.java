package com.jm.currencyexchange.domain.model;

import java.util.Objects;

public record EdgeNode(String currency, String parent, double cost) implements Comparable<EdgeNode> {

    @Override
    public int compareTo(EdgeNode o) {
        if (Objects.equals(this.cost, o.cost)) {
            return 1;
        } else {
            return Double.compare(this.cost, o.cost);
        }
    }
}

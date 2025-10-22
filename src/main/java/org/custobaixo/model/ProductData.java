package org.custobaixo.model;

import jakarta.annotation.Nonnull;

import java.math.BigDecimal;

public record ProductData(String name, BigDecimal price, String url) {
    @Override
    @Nonnull
    public String toString() {
        return String.format("%s - R$ %s (%s)", name, price, url);
    }
}



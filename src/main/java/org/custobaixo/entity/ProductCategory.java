package org.custobaixo.entity;

import lombok.Getter;

@Getter
public enum ProductCategory {
    ELETRONICOS("Eletrônicos"),
    PASSAGENS_AEREAS("Passagens Aéreas"),
    HOTEIS("Hotéis"),
    ROUPAS("Roupas"),
    LIVROS("Livros"),
    CASA_E_JARDIM("Casa e Jardim"),
    ESPORTES("Esportes"),
    SAUDE_E_BELEZA("Saúde e Beleza"),
    AUTOMOTIVO("Automotivo"),
    OUTROS("Outros");

    private final String displayName;

    ProductCategory(String displayName) {
        this.displayName = displayName;
    }
}
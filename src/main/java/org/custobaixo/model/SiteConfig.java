package org.custobaixo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SiteConfig {
    public String name;
    public String searchUrl;
    public String productSelector;
    public String nameSelector;
    public String priceSelector;
    public String urlSelector;
    public boolean requiresBaseUrl;
    public String baseUrl;
}


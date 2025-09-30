package org.custobaixo.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStatsResponse {

    private long totalActiveProducts;
    private long electronicsCount;
    private long flightsCount;
    private long hotelsCount;
    private long clothesCount;
    private long booksCount;
    private long homeGardenCount;
    private long sportsCount;
    private long healthBeautyCount;
    private long automotiveCount;
    private long othersCount;
    private LocalDateTime lastUpdated;
}
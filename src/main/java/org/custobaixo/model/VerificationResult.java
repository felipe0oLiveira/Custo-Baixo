package org.custobaixo.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class VerificationResult {
    private int successCount;
    private int errorCount;
    private int targetReachedCount;
    private int priceChangedCount;

    public static VerificationResult empty() {
        return VerificationResult.builder().build();
    }

    public VerificationResult merge(VerificationResult other) {
        return VerificationResult.builder()
                .successCount(this.successCount + other.successCount)
                .errorCount(this.errorCount + other.errorCount)
                .targetReachedCount(this.targetReachedCount + other.targetReachedCount)
                .priceChangedCount(this.priceChangedCount + other.priceChangedCount)
                .build();
    }
}



package com.linksfoundation.dq.core.processing.anonymization.standard.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Format used for specifying the pseudonymization process.
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PseudonymizationSpecs implements Specs{
    @Builder.Default
    private String hashAlgorithm = "SHA-256";
    @Builder.Default
    private boolean featureName = false;
    @Builder.Default
    private boolean featureValue = true;
}
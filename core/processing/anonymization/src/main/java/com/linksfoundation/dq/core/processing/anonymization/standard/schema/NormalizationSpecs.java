package com.linksfoundation.dq.core.processing.anonymization.standard.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Format used for specifying the normalization process.
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizationSpecs implements Specs{
    @Builder.Default
    private float mean = 0;
    @Builder.Default
    private float std = 1;
}
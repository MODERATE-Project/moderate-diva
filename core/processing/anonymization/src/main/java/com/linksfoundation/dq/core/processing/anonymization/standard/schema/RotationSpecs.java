package com.linksfoundation.dq.core.processing.anonymization.standard.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotationSpecs implements Specs{
    @Builder.Default
    private String feature = null;
    @Builder.Default
    private float theta = 0;
}
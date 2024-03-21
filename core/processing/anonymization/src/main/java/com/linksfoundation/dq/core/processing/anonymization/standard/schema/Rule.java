package com.linksfoundation.dq.core.processing.anonymization.standard.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Format used for specifying a generic rule.
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rule {

    private String name;
    private String feature;
    private Object specs;
}

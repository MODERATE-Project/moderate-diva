package com.linksfoundation.dq.core.aggregator.standard.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Format used for the specification of a dataset inside the configuration file.
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dataset {
    private String name;
    private String key;
    @Builder.Default
    private String expiration = "";
}

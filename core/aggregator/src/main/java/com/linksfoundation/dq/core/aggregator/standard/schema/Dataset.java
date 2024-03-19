package com.linksfoundation.dq.core.aggregator.standard.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

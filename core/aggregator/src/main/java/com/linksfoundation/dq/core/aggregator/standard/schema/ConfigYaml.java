package com.linksfoundation.dq.core.aggregator.standard.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Format used for the configuration file.
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigYaml {
    @Builder.Default
    private String name = "standard-aggregator";
    private List<Dataset> datasets;
}

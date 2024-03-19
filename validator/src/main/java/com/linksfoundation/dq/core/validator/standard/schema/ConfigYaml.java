package com.linksfoundation.dq.core.validator.standard.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigYaml {
    @Builder.Default
    private String name = "standard-validator";
    private List<Rule> rules;
    @Builder.Default
    private boolean optional = false;
}

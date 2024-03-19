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
public class CategoricalSpecs implements Specs{
    private List<String> values;
    @Builder.Default
    private boolean optional = false;
}

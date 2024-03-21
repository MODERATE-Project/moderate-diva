package com.linksfoundation.dq.core.validator.standard.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Format used for specifying the possible acceptable values for a variable.
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoricalSpecs implements Specs{
    private List<String> values;
    @Builder.Default
    private boolean optional = false;
}

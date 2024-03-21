package com.linksfoundation.dq.core.validator.standard.schema;

import lombok.*;

/**
 * Data Format used for specifying the domain of a variable.
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainSpecs implements Specs{
    @Builder.Default
    private Integer min = null;
    @Builder.Default
    private Integer max = null;
    @Builder.Default
    private boolean optional = false;
}

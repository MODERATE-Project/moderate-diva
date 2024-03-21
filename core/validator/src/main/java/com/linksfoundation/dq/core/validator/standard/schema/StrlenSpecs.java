package com.linksfoundation.dq.core.validator.standard.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Format used for specifying the length a string.
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrlenSpecs implements Specs{
    public enum Type {
        EXACT,
        LOWER,
        UPPER
    }

    @Builder.Default
    private Integer len = null;
    @Builder.Default
    private Type lenType = Type.EXACT;
    @Builder.Default
    private boolean optional = false;
}

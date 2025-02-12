package com.linksfoundation.dq.core.validator.standard.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Format used for specifying the data type of a variable.
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatatypeSpecs implements Specs{
    public enum Type {
        INTEGER,
        STRING,
        BOOLEAN,
        FLOAT
    }
    @Builder.Default
    private Type type = Type.INTEGER;
    @Builder.Default
    private boolean optional = false;
}

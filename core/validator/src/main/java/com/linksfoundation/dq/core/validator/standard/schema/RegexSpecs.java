package com.linksfoundation.dq.core.validator.standard.schema;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegexSpecs implements Specs{
    private String regex;
}

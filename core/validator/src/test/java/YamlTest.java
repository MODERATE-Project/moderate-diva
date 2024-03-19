import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.linksfoundation.dq.core.validator.standard.exceptions.RuleBadFormatted;
import com.linksfoundation.dq.core.validator.standard.exceptions.RuleNotRecognized;
import com.linksfoundation.dq.core.validator.standard.exceptions.YamlBadFormatted;
import com.linksfoundation.dq.core.validator.standard.schema.*;
import com.linksfoundation.dq.api.model.Sample;
import com.linksfoundation.dq.api.model.Validation;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.springframework.test.util.AssertionErrors.assertEquals;

public class YamlTest {
    private static final String DOMAIN_LABEL = "domain";
    private static final String STRLEN_LABEL = "strlen";

    @Test
    private void testYaml() {
        List<Mono<Validation>> checks = new LinkedList<>();
        for (Rule rule : this.parseYamlFile("prova.yaml")) {
            switch (rule.getName()) {
                case DOMAIN_LABEL:
                    DomainSpecs domainSpecs = (DomainSpecs) this.parseYamlObject(DOMAIN_LABEL, rule.getSpecs(), DomainSpecs.class);
                    checks.add(this.checkDomain(null, rule.getName(), rule.getFeature(), domainSpecs));
                    break;
                case STRLEN_LABEL:
                    StrlenSpecs strlenSpecs = (StrlenSpecs) this.parseYamlObject(STRLEN_LABEL, rule.getSpecs(), StrlenSpecs.class);
                    checks.add(this.checkStrlen(null, rule.getName(), rule.getFeature(), strlenSpecs));
                    break;
                default:
                    throw new RuleNotRecognized();
            }
        }

        assertEquals("cc", 1, 1);
    }

    protected List<Rule> parseYamlFile(String path) {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());

        try {
            File file = new File(getClass().getResource(path).getFile());
            ConfigYaml configYaml = om.readValue(file, ConfigYaml.class);
            return configYaml.getRules();

        } catch (IOException e) {
            throw new YamlBadFormatted();
        }
    }

    protected Object parseYamlObject(String name, Object specs, Class<? extends Specs> schema) {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        try {
            byte[] json = om.writeValueAsBytes(specs);
            return om.readValue(json, schema);
        } catch (IOException e) {
            throw new RuleBadFormatted("Rule %s bad formatted".formatted(name));
        }
    }

    protected Mono<Validation> checkDomain(Sample sample, String name, String feature, DomainSpecs specs) {

        List<Boolean> checks = new LinkedList<>();
        List<Float> values = sample.getFloatDataMap().get(feature).getElementList();

        for (Float value : values) {
            if (specs.getMin() != null && specs.getMax() != null) {
                checks.add(value >= specs.getMin() && value <= specs.getMax());
            }
            else {
                if (specs.getMax() == null) {
                    checks.add(value >= specs.getMin());
                }
                else {
                    checks.add(value <= specs.getMax());
                }
            }
        }

        return Mono.just(this.createValidation(sample.getTs(), name, DOMAIN_LABEL, checks));
    }

    protected Mono<Validation> checkStrlen(Sample sample, String name, String feature, StrlenSpecs specs) {
        List<Boolean> checks = new LinkedList<>();
        List<String> values = sample.getStringDataMap().get(feature).getElementList();

        for (String value : values) {
            switch (specs.getLenType()) {
                case EXACT -> checks.add(value.length() == specs.getLen());
                case LOWER -> checks.add(value.length() < specs.getLen());
                case UPPER -> checks.add(value.length() > specs.getLen());
            }
        }

        return Mono.just(this.createValidation(sample.getTs(), name, STRLEN_LABEL, checks));
    }

    protected Validation createValidation(long ts, String name, String type, List<Boolean> checks) {
        return Validation.newBuilder()
                .setTs(ts)
                .setValidator(name)
                .setType(type)
                .setOptional(Boolean.valueOf("false"))
                .setResult(checks.contains(false) ? Validation.Result.FAIL : Validation.Result.VALID)
                .build();
    }
}

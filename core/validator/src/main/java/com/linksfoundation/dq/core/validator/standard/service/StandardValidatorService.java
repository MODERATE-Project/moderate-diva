package com.linksfoundation.dq.core.validator.standard.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.linksfoundation.dq.api.validator.service.ValidationManager;
import com.linksfoundation.dq.api.validator.service.ValidationService;
import com.linksfoundation.dq.core.validator.standard.exceptions.RuleBadFormatted;
import com.linksfoundation.dq.core.validator.standard.exceptions.YamlBadFormatted;
import com.linksfoundation.dq.core.validator.standard.exceptions.RuleNotRecognized;
import com.linksfoundation.dq.core.validator.standard.schema.*;
import com.linksfoundation.dq.api.model.Sample;
import com.linksfoundation.dq.api.model.Validation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
@Slf4j
public class StandardValidatorService extends ValidationService {

    @Value(value = "${CONFIG_FILE}")
    private String configFile;
    private static final String DOMAIN_LABEL = "domain";
    private static final String STRLEN_LABEL = "strlen";
    private static final String DATATYPE_LABEL = "datatype";
    private static final String CATEGORICAL_LABEL = "categorical";
    private static final String MISSING_LABEL = "missing";
    private static final String REGEX_LABEL = "regex";

    protected StandardValidatorService(ValidationManager manager) {
        super(manager);
    }

    public Mono<Sample> validate(Sample sample, Flux<Validation> validations) {
        return validations
                .filter(validation -> validation.getResult() == Validation.Result.FAIL)
                .hasElements()
                .flatMap(result -> {

                    Sample.Builder ret = Sample.newBuilder(sample);
                    if (result.booleanValue()) {
                        return Mono.just(ret.setState(Sample.States.FAIL).build());
                    }
                    else {
                        return Mono.just(ret.setState(Sample.States.VALID).build());
                    }
                });
    }

    public Flux<Validation> check(Sample sample) {
        List<Mono<Validation>> checks = new LinkedList<>();
        ConfigYaml yaml = this.parseYamlFile(configFile);
        for (Rule rule : yaml.getRules()) {
            switch (rule.getName()) {
                case DOMAIN_LABEL:
                    DomainSpecs domainSpecs = (DomainSpecs) this.parseYamlObject(rule.getSpecs(), DomainSpecs.class);
                    checks.add(this.checkDomain(sample, yaml.getName(), rule.getFeature(), domainSpecs));
                    break;
                case STRLEN_LABEL:
                    StrlenSpecs strlenSpecs = (StrlenSpecs) this.parseYamlObject(rule.getSpecs(), StrlenSpecs.class);
                    checks.add(this.checkStrlen(sample, yaml.getName(), rule.getFeature(), strlenSpecs));
                    break;
                case DATATYPE_LABEL:
                    DatatypeSpecs datatypeSpecs = (DatatypeSpecs) this.parseYamlObject(rule.getSpecs(), DatatypeSpecs.class);
                    checks.add(this.checkDatatype(sample, yaml.getName(), rule.getFeature(), datatypeSpecs));
                    break;
                case CATEGORICAL_LABEL:
                    CategoricalSpecs categoricalSpecs = (CategoricalSpecs) this.parseYamlObject(rule.getSpecs(), CategoricalSpecs.class);
                    checks.add(this.checkCategorical(sample, yaml.getName(), rule.getFeature(), categoricalSpecs));
                    break;
                case MISSING_LABEL:
                    checks.add(this.checkMissing(sample, yaml.getName(), rule.getFeature()));
                    break;
                case REGEX_LABEL:
                    RegexSpecs regexSpecs = (RegexSpecs) this.parseYamlObject(rule.getSpecs(), RegexSpecs.class);
                    checks.add(this.checkRegex(sample, yaml.getName(), rule.getFeature(), regexSpecs));
                    break;
                default:
                    throw new RuleNotRecognized();
            }
        }

        return Flux.merge(checks);
    }

    protected ConfigYaml parseYamlFile(String path) {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());

        try {
            File file = new File(path);
            return om.readValue(file, ConfigYaml.class);

        } catch (IOException e) {
            throw new YamlBadFormatted();
        }
    }

    protected Object parseYamlObject(Object specs, Class<? extends Specs> schema) {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        try {
            byte[] json = om.writeValueAsBytes(specs);
            return om.readValue(json, schema);
        } catch (IOException e) {
            throw new RuleBadFormatted();
        }
    }

    protected Mono<Validation> checkDomain(Sample sample, String name, String feature, DomainSpecs specs) {

        List<Boolean> checks = new LinkedList<>();

        if (sample.getFloatDataMap().containsKey(feature)) {
            List<Float> values = sample.getFloatDataMap().get(feature).getElementList();

            for (Float value : values) {
                if (specs.getMin() != null && specs.getMax() != null) {
                    checks.add(value >= specs.getMin() && value <= specs.getMax());
                } else {
                    if (specs.getMax() == null) {
                        checks.add(value >= specs.getMin());
                    } else {
                        checks.add(value <= specs.getMax());
                    }
                }
            }

            return Mono.just(this.createValidation(sample.getTs(), name, DOMAIN_LABEL, checks));
        } else {
            return Mono.just(this.createValidation(sample.getTs(), name, DOMAIN_LABEL, List.of(false)));
        }
    }

    protected Mono<Validation> checkStrlen(Sample sample, String name, String feature, StrlenSpecs specs) {
        List<Boolean> checks = new LinkedList<>();

        if (sample.getStringDataMap().containsKey(feature)) {
            List<String> values = sample.getStringDataMap().get(feature).getElementList();
            for (String value : values) {
                switch (specs.getLenType()) {
                    case EXACT -> checks.add(value.length() == specs.getLen());
                    case LOWER -> checks.add(value.length() < specs.getLen());
                    case UPPER -> checks.add(value.length() > specs.getLen());
                }
            }

            return Mono.just(this.createValidation(sample.getTs(), name, STRLEN_LABEL, checks));
        } else {
            return Mono.just(this.createValidation(sample.getTs(), name, STRLEN_LABEL, List.of(false)));
        }
    }

    protected Mono<Validation> checkDatatype(Sample sample, String name, String feature, DatatypeSpecs specs) {
        List<Boolean> checks = new LinkedList<>();

        if (sample.getStringDataMap().containsKey(feature) ||
            sample.getFloatDataMap().containsKey(feature) ||
            sample.getBoolDataMap().containsKey(feature)) {
            switch (specs.getType()) {
                case STRING -> checks.add(
                        sample.getStringDataMap().containsKey(feature) &&
                                !sample.getFloatDataMap().containsKey(feature) &&
                                !sample.getBoolDataMap().containsKey(feature));
                case INTEGER, FLOAT -> checks.add(
                        !sample.getStringDataMap().containsKey(feature) &&
                                sample.getFloatDataMap().containsKey(feature) &&
                                !sample.getBoolDataMap().containsKey(feature));
                case BOOLEAN -> checks.add(
                        !sample.getStringDataMap().containsKey(feature) &&
                                !sample.getFloatDataMap().containsKey(feature) &&
                                sample.getBoolDataMap().containsKey(feature));
            }

            return Mono.just(this.createValidation(sample.getTs(), name, DATATYPE_LABEL, checks));
        } else {
            return Mono.just(this.createValidation(sample.getTs(), name, DATATYPE_LABEL, List.of(false)));
        }

    }

    protected Mono<Validation> checkCategorical(Sample sample, String name, String feature, CategoricalSpecs specs) {
        List<Boolean> checks = new LinkedList<>();

        if (sample.getStringDataMap().containsKey(feature)) {
            List<String> values = sample.getStringDataMap().get(feature).getElementList();

            for (String value : values) {
                checks.add(specs.getValues().contains(value));
            }

            return Mono.just(this.createValidation(sample.getTs(), name, CATEGORICAL_LABEL, checks));
        } else {
            return Mono.just(this.createValidation(sample.getTs(), name, CATEGORICAL_LABEL, List.of(false)));
        }
    }

    protected Mono<Validation> checkMissing(Sample sample, String name, String feature) {
        List<Boolean> checks = new LinkedList<>();

        if (sample.getFloatDataMap().containsKey(feature)) {
            checks.add(sample.getFloatDataMap().get(feature).getElementList().contains(Float.NaN));
            return Mono.just(this.createValidation(sample.getTs(), name, MISSING_LABEL, checks));
        }
        else {
            return Mono.just(this.createValidation(sample.getTs(), name, MISSING_LABEL, List.of(false)));
        }
    }

    protected Mono<Validation> checkRegex(Sample sample, String name, String feature, RegexSpecs specs) {
        List<Boolean> checks = new LinkedList<>();
        Pattern pattern;

        try {
            pattern = Pattern.compile(specs.getRegex());
        }
        catch (PatternSyntaxException e) {
            pattern = null;
        }

        if (sample.getStringDataMap().containsKey(feature) && pattern != null) {
            List<String> values = sample.getStringDataMap().get(feature).getElementList();
            
            for (String value : values) {
                Matcher matcher = pattern.matcher(value);
                checks.add(matcher.matches());
            }
            
            return Mono.just(this.createValidation(sample.getTs(), name, REGEX_LABEL, checks));
        }
        else {
            return Mono.just(this.createValidation(sample.getTs(), name, REGEX_LABEL, List.of(false)));
        }
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

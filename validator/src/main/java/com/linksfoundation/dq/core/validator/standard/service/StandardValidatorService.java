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

    /**
     * This method validates a given sample based on a series of validations.
     *
     * @param sample The sample to be validated. This is an instance of the Sample class.
     * @param validations A Flux stream of Validation objects that are to be applied to the sample.
     *
     * @return A Mono of the Sample object after validation. The state of the sample is set to FAIL if any of the validations fail. If all validations pass, the state of the sample is set to VALID.
    */
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

    /**
     * This method checks a given sample based on a set of rules defined in a YAML configuration file.
     *
     * @param sample The sample to be checked. This is an instance of the Sample class.
     *
     * @return A Flux stream of Validation objects that represent the result of each check on the sample.
     *
     * The method works as follows:
     * - It parses the YAML configuration file to get a list of rules.
     * - For each rule, it determines the type of the rule and performs the corresponding check on the sample.
     * - It adds the result of each check (a Mono of Validation) to a list.
     * - Finally, it merges all the Monos in the list into a Flux stream and returns it.
     *
     * The checks performed are based on the type of the rule and can be one of the following:
     * - Domain check
     * - String length check
     * - Data type check
     * - Categorical check
     * - Missing value check
     * - Regular expression check
     *
     * If a rule type is not recognized, it throws a RuleNotRecognized exception.
    */
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

    /**
     * This method parses a YAML configuration file and returns a ConfigYaml object.
     *
     * @param path The path to the YAML configuration file.
     *
     * @return A ConfigYaml object that represents the configuration defined in the YAML file.
    */
    protected ConfigYaml parseYamlFile(String path) {
        ObjectMapper om = new ObjectMapper(new YAMLFactory());

        try {
            File file = new File(path);
            return om.readValue(file, ConfigYaml.class);

        } catch (IOException e) {
            throw new YamlBadFormatted();
        }
    }

    /**
     * This method parses a YAML object and returns an instance of a specified class.
     *
     * @param specs The YAML object to be parsed. This is an instance of the Object class.
     * @param schema The class that the YAML object should be mapped to.
     *
     * @return An instance of the specified class that represents the parsed YAML object.
    */
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

    /**
     * This method checks if the values of a specific feature in a sample fall within a specified domain.
     *
     * @param sample The sample to be checked. This is an instance of the Sample class.
     * @param name The name of the check.
     * @param feature The feature in the sample to be checked.
     * @param specs The specifications of the domain. This is an instance of the DomainSpecs class.
     *
     * @return A Mono of a Validation object that represents the result of the check.
     *
     * The method works as follows:
     * - It checks if the sample contains the feature.
     * - If the sample contains the feature, it gets the list of values of the feature.
     * - For each value, it checks if it falls within the specified domain.
     * - It adds the result of each check to a list.
     * - It creates a Validation object with the results of the checks and returns it as a Mono.
     * - If the sample does not contain the feature, it returns a Validation object with a failed check.
    */
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

            return Mono.just(this.createValidation(sample.getTs(), name, feature, DOMAIN_LABEL, checks));
        } else {
            return Mono.just(this.createValidation(sample.getTs(), name, feature, DOMAIN_LABEL, List.of(false)));
        }
    }

    /**
     * This method checks if the lengths of the string values of a specific feature in a sample meet a specified condition.
     *
     * @param sample The sample to be checked. This is an instance of the Sample class.
     * @param name The name of the check.
     * @param feature The feature in the sample to be checked.
     * @param specs The specifications of the string length condition. This is an instance of the StrlenSpecs class.
     *
     * @return A Mono of a Validation object that represents the result of the check.
     *
     * The method works as follows:
     * - It checks if the sample contains the feature.
     * - If the sample contains the feature, it gets the list of string values of the feature.
     * - For each value, it checks if its length meets the specified condition (EXACT, LOWER, or UPPER).
     * - It adds the result of each check to a list.
     * - It creates a Validation object with the results of the checks and returns it as a Mono.
     * - If the sample does not contain the feature, it returns a Validation object with a failed check.
    */
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

            return Mono.just(this.createValidation(sample.getTs(), name, feature, STRLEN_LABEL, checks));
        } else {
            return Mono.just(this.createValidation(sample.getTs(), name, feature, STRLEN_LABEL, List.of(false)));
        }
    }

    /**
     * This method checks the datatype of a given feature in a sample against the expected datatype specified in the DatatypeSpecs.
     *
     * @param sample The Sample object which contains the data to be checked.
     * @param name The name of the sample.
     * @param feature The feature in the sample whose datatype needs to be checked.
     * @param specs The DatatypeSpecs object which contains the expected datatype of the feature.
     * @return A Mono<Validation> object. If the feature exists in the sample and its datatype matches the expected datatype, 
     *         the Validation object will contain a list of true values. If the feature does not exist in the sample or its 
     *         datatype does not match the expected datatype, the Validation object will contain a list with a single false value.
    */
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

            return Mono.just(this.createValidation(sample.getTs(), name, feature, DATATYPE_LABEL, checks));
        } else {
            return Mono.just(this.createValidation(sample.getTs(), name, feature, DATATYPE_LABEL, List.of(false)));
        }

    }

    /**
     * This method checks if the string values of a specific feature in a sample are within a specified set of categories.
     *
     * @param sample The sample to be checked. This is an instance of the Sample class.
     * @param name The name of the check.
     * @param feature The feature in the sample to be checked.
     * @param specs The specifications of the categorical condition. This is an instance of the CategoricalSpecs class.
     *
     * @return A Mono of a Validation object that represents the result of the check. If the feature does not exist in the sample or its 
     *         category does not match the specified categories, the Validation object will contain a list with a single false value.
    */
    protected Mono<Validation> checkCategorical(Sample sample, String name, String feature, CategoricalSpecs specs) {
        List<Boolean> checks = new LinkedList<>();

        if (sample.getStringDataMap().containsKey(feature)) {
            List<String> values = sample.getStringDataMap().get(feature).getElementList();

            for (String value : values) {
                checks.add(specs.getValues().contains(value));
            }

            return Mono.just(this.createValidation(sample.getTs(), name, feature, CATEGORICAL_LABEL, checks));
        } else {
            return Mono.just(this.createValidation(sample.getTs(), name, feature, CATEGORICAL_LABEL, List.of(false)));
        }
    }

    /**
     * This method checks if a given feature in a sample is missing.
     *
     * @param sample The Sample object which contains the data to be checked.
     * @param name The name of the sample.
     * @param feature The feature in the sample that needs to be checked for missing values.
     * @return A Mono<Validation> object. If the feature exists in the sample and does not contain any NaN values, 
     *         the Validation object will contain a list of true values. If the feature does not exist in the sample or contains NaN values, 
     *         the Validation object will contain a list with a single false value.
    */
    protected Mono<Validation> checkMissing(Sample sample, String name, String feature) {
        List<Boolean> checks = new LinkedList<>();

        if (sample.getFloatDataMap().containsKey(feature)) {
            checks.add(!sample.getFloatDataMap().get(feature).getElementList().contains(Float.NaN));
            return Mono.just(this.createValidation(sample.getTs(), name, feature, MISSING_LABEL, checks));
        }
        else {
            return Mono.just(this.createValidation(sample.getTs(), name, feature, MISSING_LABEL, List.of(false)));
        }
    }

    /**
     * This method checks if the values of a given feature in a sample match a specified regular expression.
     *
     * @param sample The Sample object which contains the data to be checked.
     * @param name The name of the sample.
     * @param feature The feature in the sample whose values need to be checked against the regular expression.
     * @param specs The RegexSpecs object which contains the regular expression to be matched.
     * @return A Mono<Validation> object. For each value of the feature in the sample, if the value matches the regular expression, 
     *         a true value is added to the list in the Validation object. If the value does not match the regular expression, 
     *         a false value is added to the list. If the feature does not exist in the sample or the regular expression is invalid, 
     *         the Validation object will contain a list with a single false value.
    */
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
            
            return Mono.just(this.createValidation(sample.getTs(), name, feature, REGEX_LABEL, checks));
        }
        else {
            return Mono.just(this.createValidation(sample.getTs(), name, feature, REGEX_LABEL, List.of(false)));
        }
    }

    /**
     * This method creates a Validation object based on the provided parameters.
     *
     * @param ts The timestamp associated with the validation.
     * @param name The name of the validator.
     * @param feature The feature that was validated.
     * @param type The type of validation performed.
     * @param checks A list of Boolean values representing the results of individual checks performed during the validation.
     * @return A Validation object. The result of the validation is set to Validation.Result.FAIL if any of the checks failed (i.e., if the list contains a false value), 
     *         and Validation.Result.VALID otherwise.
    */
    protected Validation createValidation(long ts, String name, String feature, String type, List<Boolean> checks) {
        return Validation.newBuilder()
                .setTs(ts)
                .setValidator(name)
                .setType(type)
                .setFeature(feature)
                .setOptional(Boolean.valueOf("false"))
                .setResult(checks.contains(false) ? Validation.Result.FAIL : Validation.Result.VALID)
                .build();
    }
}

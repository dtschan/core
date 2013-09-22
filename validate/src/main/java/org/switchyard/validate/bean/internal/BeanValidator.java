/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.switchyard.validate.bean.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.groups.Default;
import javax.xml.namespace.QName;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.switchyard.Exchange;
import org.switchyard.Property;
import org.switchyard.config.model.Model;
import org.switchyard.config.model.composite.ComponentModel;
import org.switchyard.config.model.composite.ComponentServiceModel;
import org.switchyard.config.model.switchyard.SwitchYardModel;
import org.switchyard.validate.BaseValidator;
import org.switchyard.validate.ValidationResult;

/**
 * Switchyard {@link org.switchyard.validate.Validator} based on JSR 303 bean validation. The validator can be
 * configured per service through Switchyard properties. Currently the following properties are implemented:
 * <ul>
 * <li>validate.bean.groups: comma separated list of fully qualified validation groups used for validation, white space
 * is trimmed. No validation takes place if the property is set to the special value "-". Default when property is
 * unspecified: {@link Default}.</li>
 * <li>validate.bean.groups.warn: Same as validate.bean.groups, except that constraint violations are logged but not
 * reported to Switchyard.</li>
 * <li>validate.bean.log.headers: headers whose fully qualified names are matched by this regular expression will be
 * logged with constraint violations. No headers will be logged if the property is set to the special value "-". Default
 * when property is unspecfied: {@link Exchange#MESSAGE_ID}.</li>
 * </ul>
 * 
 * @author Daniel Tschan <tschan@puzzle.ch>
 */
public class BeanValidator extends BaseValidator<Exchange> {

    public static final String NONE = "-";

    public static final String VALIDATION_GROUPS_PROPERTY = "validate.bean.groups";
    public static final String WARN_VALIDATION_GROUPS_PROPERTY = "validate.bean.groups.warn";
    public static final String LOG_HEADERS_PROPERTY = "validate.bean.log.headers";

    public static final String DEFAULT_LOG_HEADERS = Exchange.MESSAGE_ID;

    private static final Logger LOG = Logger.getLogger(BeanValidator.class);

    private static class ValidatorConfig {
        Class<?>[] validationGroups;
        Class<?>[] warnValidationGroups;
        Pattern logHeaders;
    }

    private Validator validator;

    Map<QName, ValidatorConfig> configMap;

    ValidatorConfig compositeConfig;

    public BeanValidator(QName qName, Model model) {
        super(qName);

        this.validator = Validation.buildDefaultValidatorFactory().getValidator();

        SwitchYardModel switchYardModel = getSwitchYardModel(model);

        QName serviceName = switchYardModel.getComposite().getQName();
        compositeConfig = new ValidatorConfig();
        compositeConfig.validationGroups =
                validationGroups(resolveProperty(switchYardModel, VALIDATION_GROUPS_PROPERTY), serviceName);
        compositeConfig.warnValidationGroups =
                validationGroups(resolveProperty(switchYardModel, WARN_VALIDATION_GROUPS_PROPERTY), serviceName);
        compositeConfig.logHeaders = logHeaders(resolveProperty(switchYardModel, LOG_HEADERS_PROPERTY));

        configMap = new HashMap<QName, ValidatorConfig>();
        List<ComponentModel> components = switchYardModel.getComposite().getComponents();
        for (ComponentModel component : components) {
            List<ComponentServiceModel> componentServices = component.getServices();
            for (ComponentServiceModel componentService : componentServices) {
                serviceName = componentService.getQName();
                ValidatorConfig validatorConfig = new ValidatorConfig();
                validatorConfig.validationGroups =
                        validationGroups(resolveProperty(componentService, VALIDATION_GROUPS_PROPERTY), serviceName);
                validatorConfig.warnValidationGroups =
                        validationGroups(resolveProperty(componentService, WARN_VALIDATION_GROUPS_PROPERTY),
                                serviceName);
                validatorConfig.logHeaders = logHeaders(resolveProperty(componentService, LOG_HEADERS_PROPERTY));

                if (validatorConfig.validationGroups != null || validatorConfig.logHeaders != null) {
                    configMap.put(serviceName, validatorConfig);
                }
            }
        }
    }

    @Override
    public ValidationResult validate(Exchange exchange) {
        Object bean = exchange.getMessage().getContent();

        ValidatorConfig validatorConfig = configMap.get(exchange.getProvider().getName());
        if (validatorConfig == null) {
            validatorConfig = compositeConfig;
        }

        Set<ConstraintViolation<Object>> violations = null;
        if (validatorConfig.warnValidationGroups != null) {
            violations = validator.validate(bean, validatorConfig.warnValidationGroups);

            if (!violations.isEmpty()) {
                logConstraintViolation(Level.WARN, constraintViolationsMessage(violations), exchange,
                        validatorConfig.warnValidationGroups, validatorConfig.logHeaders);
            }
        }

        if (validatorConfig.validationGroups != null) {
            violations = validator.validate(bean, validatorConfig.validationGroups);

            if (!violations.isEmpty()) {
                String violationMessage = constraintViolationsMessage(violations);
                logConstraintViolation(Level.ERROR, violationMessage, exchange, validatorConfig.validationGroups,
                        validatorConfig.logHeaders);

                return invalidResult(violationMessage);
            }
        }

        return validResult();
    }

    private static String resolveProperty(Model model, String key) {
        return (String) model.getModelConfiguration().getPropertyResolver().resolveProperty(key);
    }

    private static String constraintViolationsMessage(Set<ConstraintViolation<Object>> violations) {
        StringBuilder result = new StringBuilder();
        for (ConstraintViolation<Object> violation : violations) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(violation.getPropertyPath());
            result.append(" ");
            result.append(violation.getMessage());
        }

        return result.toString();
    }

    private static SwitchYardModel getSwitchYardModel(Model model) {
        Model parentModel = model.getModelParent();
        while (parentModel != null) {
            model = parentModel;
            parentModel = model.getModelParent();
        }

        return (SwitchYardModel) model;
    }

    private static Class<?>[] validationGroups(Object validationGroupsObject, QName serviceName) {
        String validationGroups = (String) validationGroupsObject;
        if (validationGroups == null || validationGroups.trim().isEmpty()) {
            return new Class<?>[] { Default.class };
        } else if (validationGroups.trim().equals(NONE)) {
            return null;
        }

        ArrayList<Class<?>> result = new ArrayList<Class<?>>();
        String[] groups = validationGroups.split(",");

        for (String group : groups) {
            Class<?> groupClass;
            try {
                groupClass = Class.forName(group);
                result.add(groupClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(serviceName + ": Can't find validation group named '" + group + "'", e);
            }
        }

        return result.toArray(new Class<?>[result.size()]);
    }

    private Pattern logHeaders(String logHeaders) {
        if (logHeaders == null || logHeaders.trim().isEmpty()) {
            return Pattern.compile(DEFAULT_LOG_HEADERS);
        } else if (logHeaders.trim().equals(NONE)) {
            return null;
        } else {
            return Pattern.compile(logHeaders, Pattern.CASE_INSENSITIVE);
        }
    }

    private static void logConstraintViolation(Level level, String violationMessage, Exchange exchange,
            Class<?>[] validationGroups, Pattern logHeaders) {
        if (LOG.isEnabled(level)) {
            StringBuilder logMessage = new StringBuilder();

            if (level.equals(Level.WARN)) {
                logMessage.append("Constraint warning: ");
            } else {
                logMessage.append("Constraint violation: ");
            }
            logMessage.append(violationMessage);

            logMessage.append(", service: ").append(exchange.getProvider().getName());
            logMessage.append(", class: ").append(exchange.getMessage().getContent().getClass().getName());

            logMessage.append(", groups: ");
            for (Class<?> validationGroup : validationGroups) {
                logMessage.append(validationGroup.getName()).append("; ");
            }

            for (Property header : exchange.getContext().getProperties()) {
                if (logHeaders.matcher(header.getName()).find()) {
                    logMessage.append(header.getName()).append(": ").append(header.getValue()).append("; ");
                }
            }

            LOG.log(level, logMessage);
        }
    }
}

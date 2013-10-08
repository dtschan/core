/*
 * Copyright (c) 2013 BLS and/or its affiliates. All rights reserved.
 * Usage is subject to license terms.
 */
package org.switchyard.validate.bean.internal;

//import static org.hamcrest.Matchers.containsString;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.namespace.QName;

import junit.framework.Assert;

import org.hibernate.validator.constraints.Email;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.switchyard.MockContext;
import org.switchyard.MockExchange;
import org.switchyard.MockService;
import org.switchyard.config.model.composite.ComponentModel;
import org.switchyard.config.model.composite.ComponentServiceModel;
import org.switchyard.config.model.composite.CompositeModel;
import org.switchyard.config.model.composite.v1.V1ComponentModel;
import org.switchyard.config.model.composite.v1.V1ComponentServiceModel;
import org.switchyard.config.model.composite.v1.V1CompositeModel;
import org.switchyard.config.model.property.PropertyModel;
import org.switchyard.config.model.property.v1.V1PropertyModel;
import org.switchyard.config.model.switchyard.SwitchYardModel;
import org.switchyard.config.model.switchyard.v1.V1SwitchYardModel;
import org.switchyard.internal.DefaultMessage;
import org.switchyard.validate.ValidationResult;

/**
 * Unit test for {@link BeanValidator}.
 * 
 * @author Daniel Tschan <tschan@puzzle.ch>
 */
public class BeanValidatorTest {
    private static final String TEST_SERVICE_NAME = "TestService";

    private SwitchYardModel _configModel;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() {
        _configModel = new V1SwitchYardModel();
        CompositeModel compositeModel = new V1CompositeModel();
        compositeModel.setName(getClass().getSimpleName());
        _configModel.setComposite(compositeModel);
    }

    @Test
    public void validationDisabled() throws Throwable {
        addComponentProperty(TEST_SERVICE_NAME, BeanValidator.VALIDATION_GROUPS_PROPERTY, "-");

        ExampleBean exampleBean = new ExampleBean(null, 0, null);
        assertValidResult(validateBean(exampleBean));
    }

    @Test
    public void validationSuccessWithDefault() throws Throwable {
        ExampleBean exampleBean = new ExampleBean("John", 42, null);
        assertValidResult(validateBean(exampleBean));
    }

    @Test
    public void validationFailWithDefault() throws Throwable {
        ExampleBean exampleBean = new ExampleBean(null, 42, null);
        assertInvalidResult(validateBean(exampleBean), "name may not be null");
    }

    @Test
    public void validationSuccessWithAgeCheck() throws Throwable {
        addComponentProperty(TEST_SERVICE_NAME, BeanValidator.VALIDATION_GROUPS_PROPERTY, AgeCheck.class.getName());

        ExampleBean exampleBean = new ExampleBean("John", 42, null);
        assertValidResult(validateBean(exampleBean));
    }


    @Test
    public void validationFailWithAgeCheck() throws Throwable {
        addComponentProperty(TEST_SERVICE_NAME, BeanValidator.VALIDATION_GROUPS_PROPERTY, AgeCheck.class.getName());

        ExampleBean exampleBean = new ExampleBean("John", 0, null);
        assertInvalidResult(validateBean(exampleBean), "age must be greater than or equal to 18");
    }

    @Test
    public void validationFailWithAgeEndEMailCheck() throws Throwable {
        addComponentProperty(TEST_SERVICE_NAME, BeanValidator.VALIDATION_GROUPS_PROPERTY, AgeCheck.class.getName() + "," + EMailCheck.class.getName());

        //        propertyMixIn.set(BeanValidator.LOG_HEADERS_PROPERTY, ".*");
        ExampleBean exampleBean = new ExampleBean("John", 42, "john");
        assertInvalidResult(validateBean(exampleBean), "email not a well-formed email address");
    }

    @Test
    public void validationSuccessWithAgeEndEMailCheck() throws Throwable {
        addComponentProperty(TEST_SERVICE_NAME, BeanValidator.VALIDATION_GROUPS_PROPERTY, AgeCheck.class.getName() + "," + EMailCheck.class.getName());

        ExampleBean exampleBean = new ExampleBean("John", 42, "john@example.net");
        assertValidResult(validateBean(exampleBean));
    }

    private static void assertValidResult(ValidationResult validationResult) {
        Assert.assertTrue(validationResult.isValid());
    }


    private static void assertInvalidResult(ValidationResult validationResult, String expectedDetail) {
        assertFalse(validationResult.isValid());
        assertEquals(expectedDetail, validationResult.getDetail());
    }

    private ValidationResult validateBean(Object bean) {
        BeanValidator beanValidator = new BeanValidator(QName.valueOf("java:java.lang.Object"), _configModel);

        MockContext context = new MockContext();
        MockExchange exchange = new MockExchange();
        exchange.setContext(context);
        exchange.setProvider(new MockService(TEST_SERVICE_NAME));
        exchange.setMessage(new DefaultMessage().setContent(bean));

        return beanValidator.validate(exchange);
    }

    private void addComponentProperty(String serviceName, String name, String value) {
        CompositeModel composite = _configModel.getComposite();

        ComponentModel component = new V1ComponentModel();
        component.setName(serviceName);
        composite.addComponent(component);

        ComponentServiceModel componentService = new V1ComponentServiceModel();
        componentService.setName(serviceName);
        component.addService(componentService);

        PropertyModel propertyModel = new V1PropertyModel(component.getTargetNamespace());
        propertyModel.setName(name);
        propertyModel.setValue(value);
        component.addProperty(propertyModel);
    }
}

class ExampleBean {
    private String name;
    private int age;
    private String email;

    public ExampleBean(String name, int age, String eMail) {
        this.name = name;
        this.age = age;
        this.email = eMail;
    }

    @NotNull
    @Size(min = 2, max = 10)
    String getName() {
        return this.name;
    }

    @Min(value = 18, groups = AgeCheck.class)
    @Max(65)
    int getAge() {
        return this.age;
    }

    @NotNull(groups = EMailCheck.class)
    @Email(groups = EMailCheck.class)
    String getEmail() {
        return this.email;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", age: " + age + ", email: " + email;
    }
}

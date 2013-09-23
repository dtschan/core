/*
 * Copyright (c) 2013 BLS and/or its affiliates. All rights reserved.
 * Usage is subject to license terms.
 */
package org.switchyard.validate.bean.internal;

import static org.hamcrest.Matchers.containsString;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.switchyard.BaseHandler;
import org.switchyard.HandlerException;
import org.switchyard.config.model.composite.ComponentModel;
import org.switchyard.config.model.composite.ComponentServiceModel;
import org.switchyard.config.model.composite.CompositeModel;
import org.switchyard.config.model.composite.v1.V1ComponentModel;
import org.switchyard.config.model.composite.v1.V1ComponentServiceModel;
import org.switchyard.config.model.property.PropertyModel;
import org.switchyard.config.model.property.v1.V1PropertyModel;
import org.switchyard.config.model.switchyard.SwitchYardModel;
import org.switchyard.test.InvocationFaultException;
import org.switchyard.test.SwitchYardRunner;
import org.switchyard.test.SwitchYardTestCaseConfig;
import org.switchyard.test.SwitchYardTestKit;
import org.switchyard.test.mixins.PropertyMixIn;

/**
 * Unit test for {@link BeanValidator}.
 * 
 * @author Daniel Tschan <tschan@puzzle.ch>
 */
@RunWith(SwitchYardRunner.class)
@SwitchYardTestCaseConfig(mixins = { PropertyMixIn.class })
public class BeanValidatorTest {
    private static final String TEST_SERVICE_NAME = "TestService";

    private SwitchYardTestKit _testKit;
    private PropertyMixIn propertyMixIn;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() {
        _testKit.registerInOnlyService(TEST_SERVICE_NAME, new BaseHandler());
    }

    @Test
    public void validationDisabled() throws Throwable {
        addComponentProperty(TEST_SERVICE_NAME, BeanValidator.VALIDATION_GROUPS_PROPERTY, "-");

        _testKit.getServiceDomain().getValidatorRegistry()
        .addValidator(new BeanValidator(QName.valueOf("java:java.lang.Object"), _testKit.getConfigModel()));

        try {
            TestBean testBean = new TestBean(null, 0, null);
            _testKit.newInvoker("TestService").inputType(QName.valueOf("java:" + TestBean.class.getName()))
            .sendInOut(testBean);
        } catch (InvocationFaultException e) {
            throw e.getCause();
        }
    }

    @Test
    public void validationSuccessWithDefault() throws Throwable {
        _testKit.getServiceDomain().getValidatorRegistry()
        .addValidator(new BeanValidator(QName.valueOf("java:java.lang.Object"), _testKit.getConfigModel()));

        try {
            TestBean testBean = new TestBean("John", 42, null);
            _testKit.newInvoker("TestService").inputType(QName.valueOf("java:" + TestBean.class.getName()))
            .sendInOut(testBean);
        } catch (InvocationFaultException e) {
            throw e.getCause();
        }
    }

    @Test
    public void validationFailWithDefault() throws Throwable {
        expectedEx.expect(HandlerException.class);
        expectedEx.expectMessage(containsString("Validator '" + BeanValidator.class.getName() + "' failed"));

        _testKit.getServiceDomain().getValidatorRegistry()
        .addValidator(new BeanValidator(QName.valueOf("java:java.lang.Object"), _testKit.getConfigModel()));

        try {
            TestBean testBean = new TestBean(null, 42, null);
            _testKit.newInvoker("TestService").inputType(QName.valueOf("java:" + TestBean.class.getName()))
            .sendInOut(testBean);
        } catch (InvocationFaultException e) {
            throw e.getCause();
        }
    }

    @Test
    public void validationSuccessWithAgeCheck() throws Throwable {
        addComponentProperty(TEST_SERVICE_NAME, BeanValidator.VALIDATION_GROUPS_PROPERTY, AgeCheck.class.getName());

        _testKit.getServiceDomain().getValidatorRegistry()
        .addValidator(new BeanValidator(QName.valueOf("java:java.lang.Object"), _testKit.getConfigModel()));

        try {
            TestBean testBean = new TestBean("John", 42, null);
            _testKit.newInvoker("TestService").inputType(QName.valueOf("java:" + TestBean.class.getName()))
            .sendInOut(testBean);
        } catch (InvocationFaultException e) {
            throw e.getCause();
        }
    }

    @Test
    public void validationFailWithAgeCheck() throws Throwable {
        expectedEx.expect(HandlerException.class);
        expectedEx.expectMessage(containsString("Validator '" + BeanValidator.class.getName() + "' failed"));

        propertyMixIn.set(BeanValidator.VALIDATION_GROUPS_PROPERTY, AgeCheck.class.getName());

        _testKit.getServiceDomain().getValidatorRegistry()
        .addValidator(new BeanValidator(QName.valueOf("java:java.lang.Object"), _testKit.getConfigModel()));

        try {
            TestBean testBean = new TestBean("John", 0, null);
            _testKit.newInvoker("TestService").inputType(QName.valueOf("java:" + TestBean.class.getName()))
            .sendInOut(testBean);
        } catch (InvocationFaultException e) {
            throw e.getCause();
        }
    }

    @Test
    public void validationFailWithAgeEndEMailCheck() throws Throwable {
        expectedEx.expect(HandlerException.class);
        expectedEx.expectMessage(containsString("Validator '" + BeanValidator.class.getName() + "' failed"));

        propertyMixIn.set(BeanValidator.VALIDATION_GROUPS_PROPERTY,
                AgeCheck.class.getName() + "," + EMailCheck.class.getName());
        propertyMixIn.set(BeanValidator.LOG_HEADERS_PROPERTY, ".*");

        _testKit.getServiceDomain().getValidatorRegistry()
        .addValidator(new BeanValidator(QName.valueOf("java:java.lang.Object"), _testKit.getConfigModel()));

        try {
            TestBean testBean = new TestBean("John", 42, "john");
            _testKit.newInvoker("TestService").inputType(QName.valueOf("java:" + TestBean.class.getName()))
            .sendInOut(testBean);
        } catch (InvocationFaultException e) {
            throw e.getCause();
        }
    }

    @Test
    public void validationSuccessWithAgeEndEMailCheck() throws Throwable {
        expectedEx.expect(HandlerException.class);
        expectedEx.expectMessage(containsString("Validator '" + BeanValidator.class.getName() + "' failed"));

        propertyMixIn.set(BeanValidator.VALIDATION_GROUPS_PROPERTY,
                AgeCheck.class.getName() + "," + EMailCheck.class.getName());

        _testKit.getServiceDomain().getValidatorRegistry()
        .addValidator(new BeanValidator(QName.valueOf("java:java.lang.Object"), _testKit.getConfigModel()));

        try {
            TestBean testBean = new TestBean("John", 0, "john@example.net");
            _testKit.newInvoker("TestService").inputType(QName.valueOf("java:" + TestBean.class.getName()))
            .sendInOut(testBean);
        } catch (InvocationFaultException e) {
            throw e.getCause();
        }
    }

    private void addComponentProperty(String serviceName, String name, String value) {
        SwitchYardModel model = _testKit.getConfigModel();
        CompositeModel composite = model.getComposite();

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

class TestBean {
    private String name;
    private int age;
    private String email;

    public TestBean(String name, int age, String eMail) {
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
    // @Email(groups = EMailCheck.class)
    String getEmail() {
        return this.email;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", age: " + age + ", email: " + email;
    }
}

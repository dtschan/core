/*
 * Copyright (c) 2013 BLS and/or its affiliates. All rights reserved.
 * Usage is subject to license terms.
 */
package org.switchyard.validate.config.model.v1;

import javax.xml.namespace.QName;

import org.switchyard.config.Configuration;
import org.switchyard.config.model.Descriptor;
import org.switchyard.config.model.validate.ValidateModel;
import org.switchyard.config.model.validate.v1.V1BaseValidateModel;
import org.switchyard.validate.internal.ValidatorFactoryClass;
import org.switchyard.validate.bean.internal.BeanValidatorFactory;
import org.switchyard.validate.config.model.BeanValidateModel;
import org.switchyard.validate.config.model.JavaValidateModel;

/**
 * A version 1 BeanValidationModel. Based on {@link JavaValidateModel}.
 * 
 * @author Daniel Tschan <tschan@puzzle.ch>
 */
@ValidatorFactoryClass(BeanValidatorFactory.class)
public class V1BeanValidateModel extends V1BaseValidateModel implements BeanValidateModel {

    /**
     * Constructs a new V1BeanValidateModel.
     */
    public V1BeanValidateModel() {
        super(new QName(ValidateModel.DEFAULT_NAMESPACE, ValidateModel.VALIDATE + '.' + BEAN));
    }

    /**
     * Constructs a new V1BeanValidateModel with the specified Configuration and Descriptor.
     * 
     * @param config
     *            the Configuration
     * @param desc
     *            the Descriptor
     */
    public V1BeanValidateModel(Configuration config, Descriptor desc) {
        super(config, desc);
    }
}

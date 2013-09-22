/*
 * Copyright (c) 2013 BLS and/or its affiliates. All rights reserved.
 * Usage is subject to license terms.
 */
package org.switchyard.validate.config.model;

import org.switchyard.config.model.validate.ValidateModel;

/**
 * A "validate.bean" configuration model.
 * 
 * @author Daniel Tschan <tschan@puzzle.ch>
 */
public interface BeanValidateModel extends ValidateModel {

    /** The "bean" name. */
    public static final String BEAN = "bean";
}

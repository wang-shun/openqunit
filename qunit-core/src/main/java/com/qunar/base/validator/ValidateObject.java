/*
 * $$Id$$
 * Copyright (c) 2011 Qunar.com. All Rights Reserved.
 */

package com.qunar.base.validator;

/**
 * 待验证数据的包装
 *
 * Created by JarnTang at 12-8-27 下午6:14
 *
 * @author  JarnTang
 */
public class ValidateObject {

    Object value;
    boolean order;
    boolean continueValidate;

    public ValidateObject(Object value) {
        this.value = value;
        continueValidate = true;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isOrder() {
        return order;
    }

    public void setOrder(boolean order) {
        this.order = order;
    }

    public boolean isContinueValidate() {
        return continueValidate;
    }

    public void setContinueValidate(boolean continueValidate) {
        this.continueValidate = continueValidate;
    }

}

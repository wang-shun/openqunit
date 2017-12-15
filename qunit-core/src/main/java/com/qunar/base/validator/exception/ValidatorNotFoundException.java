/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.exception;

/**
 * 最则校验器不存在异常信息
 *
 * Created by JarnTang at 12-5-29 下午2:16
 *
 * @author  JarnTang
 */
public class ValidatorNotFoundException extends RuntimeException{

    public ValidatorNotFoundException(String message){
        super(message);
    }

}

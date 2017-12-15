package com.qunar.base.validator.validate;

import com.alibaba.fastjson.JSON;
import com.qunar.base.validator.JsonSchema;
import com.qunar.base.validator.schema.Schema;
import com.qunar.base.validator.util.ReaderUtil;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * javaCC实现的校验器，通过给定的校验规则对目标JSON进行校验
 *
 * @author  JarnTang
 * @version V1.0
 */
public class JavaCCSchema implements JsonSchema {

    private Schema<Object> schema;

    public JavaCCSchema(Schema<Object> schema) {
        this.schema = schema;
    }

    @Override
    public void validate(String json) {
        Object jsonObject = parse(json);
        schema.validate("$", jsonObject);
    }

    private Object parse(String json) {
        try {
            return tryParse(json);
        } catch (Exception e) {
            return tryParse(String.format("\"%s\"", json));
        }
    }

    private Object tryParse(String json) {
        try {
            return JSON.parse(json);
        } catch (Exception e) {
            throw new RuntimeException("接口返回的json语法错误，请检查:" + e.getMessage(), e);
        }
    }

    @Override
    public void validate(InputStream jsonStream) {
        validate(ReaderUtil.reader(jsonStream));
    }

    @Override
    public void validate(Reader jsonReader) {
        validate(ReaderUtil.reader(jsonReader));
    }

    @Override
    public void validate(URL jsonURL) {
        validate(ReaderUtil.reader(jsonURL));
    }

}

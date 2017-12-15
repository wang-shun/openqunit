/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.schema;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qunar.base.validator.builder.ValidatorBuilder;
import com.qunar.base.validator.matchers.QAssert;
import com.qunar.base.validator.validators.Validator;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.qunar.base.validator.util.LineUtil.separator;

/**
 * 对象规则引擎，主要负责JSON对象的对比
 * <p/>
 * Created by JarnTang at 12-5-28 下午4:15
 *
 * @author  JarnTang
 */
public class ObjectRuleSchema extends Schema<Object> {

    private Map<String, Schema<Object>> schemas;

    @Override
    public void validate(String parentKey, Object object) {
        if (object == null) {
            if (schemas != null && schemas.size() > 0) {
                QAssert.fail(parentKey, getOriginalSchemaStr(), object);
            }
            return;
        }
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            for (Map.Entry<String, Schema<Object>> entry : getSchemas().entrySet()) {
                String key = entry.getKey();
                Schema<Object> value = entry.getValue();
                String concurrentKeyPath = getKey(parentKey, key);
                if (object == null) {
                    if (getAttrRule().size() != 0 || value != null) {
                        Assert.fail("path<" + concurrentKeyPath + "> schema is " + value + separator +
                                " but parent path<" + parentKey + "> value is null." + separator);
                    }
                    return;
                }
                List<Attr> attrList = value.getAttrRule();
                Validator validator = new ValidatorBuilder().withAttrs(attrList, concurrentKeyPath, value)
                        .withSchema(concurrentKeyPath, value).build();
                regexMatchValidate(jsonObject, key, parentKey, validator);
            }
        } else {
            QAssert.fail(parentKey, schemas, object);
        }
    }

    @Override
    public String getOriginalSchemaStr() {
        StringBuilder sb = new StringBuilder("{");
        if (schemas != null) {
            int index = 0;
            for (Map.Entry<String,Schema<Object>> entry : schemas.entrySet()) {
                if (index++ != 0) {
                    sb.append(",");
                }
                sb.append('"').append(entry.getKey()).append('"').append(":");
                sb.append(entry.getValue().getOriginalSchemaStr());
            }
        }
        sb.append("}").append(getAttrOriginalStr());
        return trimColon(sb.toString());
    }

    private void regexMatchValidate(JSONObject object, String key, String parentKey, Validator validator) {
        List<Object> objectList = matchSchema(key, object);
        if (StringUtils.isNotBlank(key) && objectList.size() == 0) {
            if (!object.containsKey(key) && validator.isNecessaryCheck()) {
                QAssert.fail(getKey(parentKey, key) + " not exist", getOriginalSchemaStr(), JSON.toJSONString(object));
            } else {
                validator.validate(object.get(key));
            }
        }
        for (Object o : objectList) {
            validator.validate(o);
        }
    }

    private List<Object> matchSchema(String key, JSONObject jsonObject) {
        List<Object> result = new ArrayList<Object>();
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            String schemaKey = entry.getKey();
            if (hasMatch(key, schemaKey)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    private boolean hasMatch(String regex, String value) {
        Matcher matcher = Pattern.compile(regex).matcher(value);
        if (matcher.find()) {
            String group = matcher.group();
            return value.length() == group.length();
        }
        return false;
    }


    public synchronized Map<String, Schema<Object>> getSchemas() {
        if (schemas == null) {
            schemas = new HashMap<String, Schema<Object>>();
        }
        return schemas;
    }

    public void addSchema(String schemaName, Schema schema) {
        getSchemas().put(schemaName, schema);
    }


    @Override
    public String toString() {
        return "ObjectRuleSchema{attrRule=" + getAttrRule() + ", schemas=" + schemas + '}';
    }

}

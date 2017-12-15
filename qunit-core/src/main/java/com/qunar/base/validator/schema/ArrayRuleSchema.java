/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.schema;

import com.alibaba.fastjson.JSONArray;
import com.qunar.base.validator.JsonValidator;
import com.qunar.base.validator.matchers.QAssert;
import com.qunar.base.validator.validators.DisorderValidator;
import com.qunar.base.validator.validators.OrderValidator;
import com.qunar.base.validator.validators.Validator;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * 数组规则引擎，主要负责JSON数组的对比
 * <p/>
 * Created by JarnTang at 12-5-28 下午4:49
 *
 * @author  JarnTang
 */
public class ArrayRuleSchema extends Schema<Object> {

    List<Schema<Object>> schemaList;

    public ArrayRuleSchema() {
    }

    public ArrayRuleSchema(List<Schema<Object>> schemas) {
        schemaList = schemas;
    }

    @Override
    public void validate(String parentKey, Object object) {
        QAssert.assertThat("期望" + parentKey + "是数组类型", object, instanceOf(JSONArray.class));

        JSONArray array = (JSONArray) object;
        if (getSchemaList().size() != 0) {
            String reason = parentKey + "实际得到的数组大小与期望的数组大小不匹配";
            QAssert.assertThat(reason, array.size(), is(getSchemaList().size()), getOriginalSchemaStr(), object);
        }
        Validator validator = new DisorderValidator(null);
        if (JsonValidator.arrayDefaultOrderValidate) {
            validator = new OrderValidator(null);
        }
        validator.setCurrentKeyPath(parentKey);
        validator.setValueSchema(this);
        validator.validate(object);
    }

    @Override
    public String getOriginalSchemaStr() {
        List<Object> result = new ArrayList<Object>();
        for (int index = 0; index < getSchemaList().size(); index++) {
            result.add(getSchemaList().get(index).getOriginalSchemaStr());
        }
        return trimColon(result.toString() + getAttrOriginalStr());
    }

    public List<Schema<Object>> getSchemaList() {
        if (schemaList == null) {
            schemaList = new ArrayList<Schema<Object>>();
        }
        return schemaList;
    }

    public void addSchema(Schema<Object> schema) {
        getSchemaList().add(schema);
    }

    @Override
    public String toString() {
        return "ArrayRuleSchema{attrRule=" + getAttrRule() + ", schemaList=" + schemaList + '}';
    }

}

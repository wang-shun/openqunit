package com.qunar.base.validator.validators;

import com.qunar.base.validator.exception.UnSupportedTypeException;
import com.qunar.base.validator.matchers.QAssert;
import com.qunar.base.validator.schema.ArrayRuleSchema;
import com.qunar.base.validator.schema.Schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: zhaohuiyu
 * Date: 2/21/13
 * Time: 8:03 PM
 */
public abstract class ArrayValidator extends Validator {

    @Override
    public void doValidate(Object object) {
        if (!(getValueSchema() instanceof ArrayRuleSchema)) {
            throw new UnSupportedTypeException(String.format("数组对比要求json里是数组类型，请检查json串 <%s>",
                    getValueSchema().getClass().getCanonicalName()));
        }
        if (object != null) {
            if (object.getClass().isArray() || object instanceof List) {
                ArrayRuleSchema ars = (ArrayRuleSchema) getValueSchema();
                List<Schema<Object>> schemaList = ars.getSchemaList();
                List<Object> actual = transformObjectToList(object);

                arrayValidate(schemaList, actual);
            } else {
                QAssert.fail("数组对比失败", "数组类型", object.getClass().getCanonicalName());
            }
        } else {
            QAssert.fail(getCurrentKeyPath() + "数组对比失败", getValueSchema().toString(), null);
        }
    }

    protected abstract void arrayValidate(List<Schema<Object>> schemas, List<Object> actual);

    private List<Object> transformObjectToList(Object object) {
        List<Object> result = new ArrayList<Object>();
        if (object.getClass().isArray()) {
            Object[] array = (Object[]) object;
            Collections.addAll(result, array);
        } else if (object instanceof List) {
            result.addAll((List<?>) object);
        }
        return result;
    }
}

/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.base.validator.validators;

import com.qunar.base.validator.matchers.QAssert;
import com.qunar.base.validator.schema.Schema;

import java.util.ArrayList;
import java.util.List;

import static com.qunar.base.validator.schema.Schema.getKey;

/**
 * 对两个列表或数组的内容进行无序对比，如果内容不一致，则抛AssertionError异常
 * 注:只能用于数组对比,如需要有序对比，请看 {@link OrderValidator}
 * <br><br>
 * Created by JarnTang at 12-6-15 下午3:45
 *
 * @author  JarnTang
 */
public class DisorderValidator extends ArrayValidator {
    public static final String VALIDATOR_NAME = "noorder";

    public DisorderValidator(List<Object> list) {

    }

    @Override
    protected void arrayValidate(List<Schema<Object>> schemas, List<Object> actual) {
        if (schemas.size() == 0 && actual.size() != 0) {
            QAssert.fail("期望数组大小为 0，实际得到的是 " + actual.size(), getValueSchema().getOriginalSchemaStr(), actual);
        }
        for (Schema<Object> schema : schemas) {
            String errMsg = "";
            List<Integer> equalsSchema = new ArrayList<Integer>();
            boolean isValidate = false;
            for (int i = 0; i < actual.size(); i++) {
                if (!equalsSchema.contains(i)) {
                    try {
                        schema.validate(getKey(getCurrentKeyPath(), i), actual.get(i));
                    } catch (ClassCastException e) {
                        //这里为了不按顺序校验数组里的元素，对数组的校验进行乱序比较，这样可能导致不匹配的类型之间进行比较
                        errMsg = e.getMessage();
                        continue;
                    } catch (AssertionError assertionError) {
                        errMsg = assertionError.getMessage();
                        continue;
                    }
                    errMsg = null;
                    isValidate = true;
                    equalsSchema.add(i);
                    break;
                }
            }
            if (!isValidate) {
                String message = String.format("列表无序对比失败%s", errMsg);
                QAssert.fail(message, schema.getOriginalSchemaStr(), actual);
            }
        }
    }

    @Override
    public String name() {
        return VALIDATOR_NAME;
    }
}

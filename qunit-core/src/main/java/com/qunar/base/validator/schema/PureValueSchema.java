package com.qunar.base.validator.schema;

import com.qunar.base.validator.builder.ValidatorBuilder;
import com.qunar.base.validator.validators.ValueValidator;

import java.util.ArrayList;

/**
 * 具体的值对比引擎，负责JSON中Java基本类型的对比
 * <p/>
 * Created by JarnTang at 12-5-28 下午4:15
 *
 * @author  JarnTang
 */
public class PureValueSchema extends Schema<Object> {

    private Object value;

    public PureValueSchema(Object value) {
        this.value = value;
    }

    @Override
    public void validate(String parentKey, Object object) {
        ValueValidator validator = new ValueValidator(parentKey, value);
        validator.setValueSchema(this);
        new ValidatorBuilder().withAttrs(new ArrayList<Attr>(attrRule), parentKey, this).withOtherValidator(validator)
                .build().validate(object);
    }

    @Override
    public String getOriginalSchemaStr() {
        if (value == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (!(value instanceof Any)) {
            if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append('"').append(value).append('"');
            }
        }
        sb.append(getAttrOriginalStr());
        return trimColon(sb.toString());
    }

    @Override
    public String toString() {
        return "PureValueSchema{attrRule=" + getAttrRule() + ", value=" + value + '}';
    }
}

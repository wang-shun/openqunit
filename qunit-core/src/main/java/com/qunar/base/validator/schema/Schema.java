package com.qunar.base.validator.schema;

import com.qunar.base.validator.validators.OptionalValidator;

import java.util.ArrayList;
import java.util.List;

public abstract class Schema<T> {

    protected List<Attr> attrRule;

	public abstract void validate(String parentKey, T object);

    public abstract String getOriginalSchemaStr();

//    protected void validateRule(Object object) {
//        for (Attr attr : getAttrRule()) {
//            String attrName = attr.getAttrName();
//            List<Object> arguments = attr.getArguments();
//            ValidatorFactory.getValidator(attrName, arguments).doValidate(object);
//        }
//    }

    public static String getKey(String parentKey, String currentKey) {
        return parentKey + "." + currentKey;
    }

    public static String getKey(String parentKey, int index) {
        return parentKey + "[" + index + "]";
    }

    public List<Attr> getAttrRule() {
        if (attrRule == null) {
            return new ArrayList<Attr>(0);
        }
        return attrRule;
    }

    public void setAttrRule(List<Attr> attrRule) {
        this.attrRule = attrRule;
    }

    protected String getAttrOriginalStr() {
        StringBuilder sb = new StringBuilder();
        if (getAttrRule()!=null) {
            for (Attr attr : getAttrRule()) {
                sb.append(":").append(attr.getAttrName());
                List<Object> arguments = attr.getArguments();
                if (arguments != null && arguments.size() > 0) {
                    sb.append("(");
                    for (int index = 0; index < arguments.size(); index++) {
                        if (index != 0) {
                            sb.append(",");
                        }
                        Object value = arguments.get(index);
                        if (value instanceof Number || value instanceof Boolean) {
                            sb.append(value);
                        } else {
                            sb.append('"').append(value).append('"');
                        }
                    }
                    sb.append(")");
                }
            }
        }
        return sb.toString();
    }

    public boolean isNecessaryCheck() {
        if (attrRule == null || attrRule.size() == 0) {
            return true;
        }
        for (Attr attr : attrRule) {
            if (OptionalValidator.VALIDATOR_NAME.equals(attr.getAttrName())) {
                return false;
            }
        }
        return true;
    }

    protected String trimColon(String str) {
        if (str != null && str.startsWith(":")) {
            str = str.substring(1);
        }
        return str;
    }

}

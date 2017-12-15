package com.qunar.base.validator;

import com.qunar.base.validator.exception.JsonSyntaxError;
import com.qunar.base.validator.matchers.QAssert;
import com.qunar.base.validator.schema.javacc.TokenMgrError;
import com.qunar.base.validator.util.ReflectionUtils;
import com.qunar.base.validator.validate.JavaCCSchemaProvider;
import org.apache.commons.lang.StringUtils;

import static com.qunar.base.validator.util.StringUtil.deleteBlank;

/**
 * JSON校验器，根据json描述验证规则对指定的json串进行校验，是否符合规则描述
 *
 * @author  JarnTang
 * @version V1.0
 */
public class JsonValidator {

    public static Boolean arrayDefaultOrderValidate = Boolean.FALSE;

    private static final JSONSchemaProvider javaCCProvider = new JavaCCSchemaProvider();

    /**
     * 根据json描述验证规则对指定的json串进行校验
     *
     * @param schema 验证规则描述
     * @param json   待校验的json串
     * @return 校验结果
     */
    public static void validate(String schema, String json) {
        if (StringUtils.equals(schema, json)){
            return;
        }
        if (StringUtils.isBlank(schema)) {
            if (StringUtils.isNotBlank(json)) {
                QAssert.fail("数据对比失败", schema, json);
            }
            return;
        }
        try {
            createSchema(schema).validate(deleteBlank(json));
        } catch (AssertionError ae) {
            StringBuilder sb = new StringBuilder(ae.getMessage());
            sb.append("response: ").append(json).append("\n");
            ReflectionUtils.setFieldValue(ae, "detailMessage", sb.toString());
            throw ae;
        }
    }

    public static JsonSchema createSchema(String schema) {
        try {
            return javaCCProvider.getSchema(deleteBlank(schema));
        } catch (TokenMgrError tokenMgrError) {
            String message = "期望值不符合schema要求，语法错误";
            message += "\n Schema: " + schema;
            message += "\nMessage: " + tokenMgrError.getMessage();
            throw new JsonSyntaxError(message, tokenMgrError);
        }
    }
}
package com.qunar.base.validator;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * 解析JSON校验规则的提供者
 *
 * @author  JarnTang
 * @version V1.0
 */
public interface JSONSchemaProvider {

    JsonSchema getSchema(String schema);

    JsonSchema getSchema(InputStream schemaStream);

    JsonSchema getSchema(Reader schemaReader);

    JsonSchema getSchema(URL schemaURL);
}

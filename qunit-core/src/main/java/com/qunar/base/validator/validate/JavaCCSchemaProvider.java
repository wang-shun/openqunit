package com.qunar.base.validator.validate;

import com.qunar.base.validator.JSONSchemaProvider;
import com.qunar.base.validator.JsonSchema;
import com.qunar.base.validator.exception.JSONSchemaException;
import com.qunar.base.validator.schema.Schema;
import com.qunar.base.validator.schema.javacc.JsonParserJavacc;
import com.qunar.base.validator.util.ReaderUtil;
import org.hamcrest.Matchers;
import org.junit.Assert;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

/**
 * the provider for read json schema
 *
 * @author  JarnTang
 * @version V1.0
 */
public class JavaCCSchemaProvider implements JSONSchemaProvider {

    @Override
    public JsonSchema getSchema(String schema) {
        return new JavaCCSchema(getSchemaByJavaCC(schema));
    }

    @Override
    public JsonSchema getSchema(InputStream schemaStream) {
        String schema = ReaderUtil.reader(schemaStream);
        return new JavaCCSchema(getSchemaByJavaCC(schema));
    }

    @Override
    public JsonSchema getSchema(Reader schemaReader) {
        String schema = ReaderUtil.reader(schemaReader);
        return new JavaCCSchema(getSchemaByJavaCC(schema));
    }

    @Override
    public JsonSchema getSchema(URL schemaURL) {
        String schema = ReaderUtil.reader(schemaURL);
        return new JavaCCSchema(getSchemaByJavaCC(schema));
    }

    private Schema<Object> getSchemaByJavaCC(String schema){
        try {
            Assert.assertThat(schema, Matchers.notNullValue());
            return new JsonParserJavacc(new StringReader(schema)).parse();
        } catch (Exception e) {
            throw new JSONSchemaException("resolve the schema error", e);
        }
    }
}

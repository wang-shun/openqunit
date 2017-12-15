package com.qunar.base.validator;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * Translates <code>JsonNode</code> type into types as defined
 * in the json specification<br/>
 *
 * @author  JarnTang
 * @version V1.0
 */
public interface JsonSchema {

    void validate(String json);

    void validate(InputStream jsonStream);

    void validate(Reader jsonReader);

    void validate(URL jsonURL);
}
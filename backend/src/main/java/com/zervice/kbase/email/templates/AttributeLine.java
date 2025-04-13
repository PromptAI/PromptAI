package com.zervice.kbase.email.templates;

import com.zervice.kbase.email.templates.model.HtmlTextEmail;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class AttributeLine implements TemplateLine {

    Map<String, String> _map = new LinkedHashMap<>();
    String _keyWidth;
    String _valueWidth;

    @Getter(AccessLevel.PRIVATE)
    EmailTemplateBuilder.EmailTemplateConfigBuilder _builder;

    AttributeLine(EmailTemplateBuilder.EmailTemplateConfigBuilder builder) {
        this._builder = builder;
    }

    public AttributeLine map(Map<String, String> map) {
        map.putAll(map);
        return this;
    }

    public AttributeLine keyValue(String key, String value) {
        _map.put(key, value);
        return this;
    }

    /**
     * width in pixel
     */
    public AttributeLine keyWidth(int keyWidth) {
        this._keyWidth = String.valueOf(keyWidth);
        return this;
    }

    /**
     * width in percentage please add %
     */
    public AttributeLine keyWidth(String keyWidth) {
        this._keyWidth = keyWidth;
        return this;
    }

    /**
     * width in pixel
     */
    public AttributeLine valueWidth(int valueWidth) {
        this._valueWidth = String.valueOf(valueWidth);
        return this;
    }

    /**
     * width in percentage please add %
     */
    public AttributeLine valueWidth(String valueWidth) {
        this._valueWidth = valueWidth;
        return this;
    }

    @Override
    public TemplateLineType getType() {
        return TemplateLineType.ATTRIBUTE;
    }

    @Override
    public EmailTemplateBuilder.EmailTemplateConfigBuilder and() {
        return _builder;
    }

    @Override
    public HtmlTextEmail build() {
        return _builder.build();
    }
}

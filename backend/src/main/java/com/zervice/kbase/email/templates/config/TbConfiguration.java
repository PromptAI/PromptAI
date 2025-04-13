package com.zervice.kbase.email.templates.config;

import com.zervice.kbase.email.templates.config.config.*;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbConfiguration {

    static final TbConfiguration DEFAULT = new TbConfiguration(TbFontConfig.newInstance(),
            TbTextConfig.newInstance(),
            TbButtonConfig.newInstance(),
            TbAttributeConfig.newInstance(),
            TbBoxConfig.newInstance(),
            TbTableConfig.newInstance(),
            TbBodyConfig.newInstance(),
            TbHeaderConfig.newInstance(),
            TbContentConfig.newInstance(),
            TbFooterConfig.newInstance());

    public static final TbConfiguration newInstance() {
        return new TbConfiguration(DEFAULT);
    }

    private TbFontConfig _font;
    private TbTextConfig _text;
    private TbButtonConfig _button;
    private TbAttributeConfig _attribute;
    private TbBoxConfig _box;
    private TbTableConfig _table;
    private TbBodyConfig _body;
    private TbHeaderConfig _header;
    private TbContentConfig _content;
    private TbFooterConfig _footer;

    public TbConfiguration(TbConfiguration other) {
        this._font = new TbFontConfig(other._font);
        this._text = new TbTextConfig(other._text);
        this._button = new TbButtonConfig(other._button);
        this._attribute = new TbAttributeConfig(other._attribute);
        this._box = new TbBoxConfig(other._box);
        this._table = new TbTableConfig(other._table);
        this._body = new TbBodyConfig(other._body);
        this._header = new TbHeaderConfig(other._header);
        this._content = new TbContentConfig(other._content);
        this._footer = new TbFooterConfig(other._footer);
    }
}

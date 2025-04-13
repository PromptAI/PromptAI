package com.zervice.kbase.email.templates.config.config;

import com.zervice.kbase.email.templates.config.base.TbFont;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbFontConfig {

    static TbFontConfig DEFAULT = new TbFontConfig("@import url('https://fonts.googleapis.com/css?family=Nunito+Sans:400,700&display=swap');",
            "'Nunito Sans', Helvetica, Arial, sans-serif",
            new TbFont("22px", "#333333"),
            new TbFont("16px", "#333333"),
            new TbFont("14px", "#333333"),
            new TbFont("16px", "#333333"),
            new TbFont("13px", "#6B6E76"));

    public static final TbFontConfig newInstance() {
        return new TbFontConfig(DEFAULT);
    }

    private String _cssImport;
    private String _family;
    private TbFont _h1;
    private TbFont _h2;
    private TbFont _h3;
    private TbFont _table;
    private TbFont _sub;

    public TbFontConfig(TbFontConfig other) {
        this._cssImport = other._cssImport;
        this._family = other._family;
        this._h1 = new TbFont(other._h1);
        this._h2 = new TbFont(other._h2);
        this._h3 = new TbFont(other._h3);
        this._table = new TbFont(other._table);
        this._sub = new TbFont(other._sub);
    }
}

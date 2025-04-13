package com.zervice.kbase.email.templates.config.config;

import com.zervice.kbase.email.templates.config.base.TbFont;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper=false)
public class TbTextConfig extends TbFont {

    static final TbTextConfig DEFAULT = new TbTextConfig(new TbFont("16px", "#51545E"),
            ".4em 0 1.1875em",
            "1.625",
            "#3869D4");

    public static final TbTextConfig newInstance() {
        return new TbTextConfig(DEFAULT);
    }

    private String _margin;
    private String _lineHeight;
    private String _linkColor;

    public TbTextConfig(TbFont font, String margin, String lineHeight, String linkColor) {
        super(font);
        this._margin = margin;
        this._lineHeight = lineHeight;
        this._linkColor = linkColor;
    }

    public TbTextConfig(TbTextConfig other) {
        super(other);
        this._margin = other._margin;
        this._lineHeight = other._lineHeight;
        this._linkColor = other._linkColor;
    }
}

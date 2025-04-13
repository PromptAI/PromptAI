package com.zervice.kbase.email.templates.config.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbHeaderConfig {

    static final TbHeaderConfig DEFAULT = new TbHeaderConfig("25px 0",
            "center",
            "16px",
            "bold",
            "#A8AAAF",
            "0 1px 0 white");

    public static final TbHeaderConfig newInstance() {
        return new TbHeaderConfig(DEFAULT);
    }

    private String _padding;
    private String _align;
    private String _size;
    private String _weight;
    private String _color;
    private String _textShadow;

    public TbHeaderConfig(TbHeaderConfig other) {
        this._padding = other._padding;
        this._align = other._align;
        this._size = other._size;
        this._weight = other._weight;
        this._color = other._color;
        this._textShadow = other._textShadow;
    }
}

package com.zervice.kbase.email.templates.config.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbAttributeConfig {

    static final TbAttributeConfig DEFAULT = new TbAttributeConfig("0 0 21px",
            "#F4F4F7",
            "16px");

    public static final TbAttributeConfig newInstance() {
        return new TbAttributeConfig(DEFAULT);
    }

    private String _margin;
    private String _background;
    private String _padding;

    public TbAttributeConfig(TbAttributeConfig other) {
        this._margin = other._margin;
        this._background = other._background;
        this._padding = other._padding;
    }
}

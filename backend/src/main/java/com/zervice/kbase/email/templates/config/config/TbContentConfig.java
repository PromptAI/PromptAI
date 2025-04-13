package com.zervice.kbase.email.templates.config.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbContentConfig {

    static final TbContentConfig DEFAULT = new TbContentConfig("#FFFFFF",
            570,
            false,
            "35px");

    public static final TbContentConfig newInstance() {
        return new TbContentConfig(DEFAULT);
    }

    private String _background;
    /**
     * width of inner-content
     */
    private Integer _width;
    /**
     * table around content - full width, then white till edges otherwise gray
     */
    private boolean _full;
    private String _padding;

    public TbContentConfig(TbContentConfig other) {
        this._background = other._background;
        this._width = other._width;
        this._full = other._full;
        this._padding = other._padding;
    }
}

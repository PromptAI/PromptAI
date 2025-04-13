package com.zervice.kbase.email.templates.config.config;

import com.zervice.kbase.email.templates.config.base.TbBackgroundColor;
import com.zervice.kbase.email.templates.config.base.TbBorderDetailed;
import com.zervice.kbase.email.templates.styling.ColorStyle;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper=false)
public class TbButtonConfig extends TbBackgroundColor {

    static final TbButtonConfig DEFAULT = new TbButtonConfig(new TbBorderDetailed("10px", "18px", "10px", "18px"),
            "3px", "0 2px 3px rgba(0, 0, 0, 0.16)",
            new TbBackgroundColor(ColorStyle.BLUE, ColorStyle.WHITE));

    public static final TbButtonConfig newInstance() {
        return new TbButtonConfig(DEFAULT);
    }

    private TbBorderDetailed _border;
    private String _borderRadius;
    private String _boxShadow;

    public TbButtonConfig(TbButtonConfig other) {
        super(other);
        this._border = new TbBorderDetailed(other._border);
        this._borderRadius = other._borderRadius;
        this._boxShadow = other._boxShadow;
    }

    public TbButtonConfig(TbBorderDetailed border, String borderRadius, String boxShadow, TbBackgroundColor defaultColor) {
        super(defaultColor);
        this._border = border;
        this._borderRadius = borderRadius;
        this._boxShadow = boxShadow;
    }
}

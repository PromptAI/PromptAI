package com.zervice.kbase.email.templates.config.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TbBackgroundColor {
    private String _background;
    private String _color;

    public TbBackgroundColor(TbBackgroundColor other) {
        this._background = other._background;
        this._color = other._color;
    }
}

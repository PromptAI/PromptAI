package com.zervice.kbase.email.templates.config.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TbBorderDetailed {
    private String _top;
    private String _right;
    private String _bottom;
    private String _left;

    public TbBorderDetailed(TbBorderDetailed other) {
        this._top = other._top;
        this._right = other._right;
        this._bottom = other._bottom;
        this._left = other._left;
    }
}

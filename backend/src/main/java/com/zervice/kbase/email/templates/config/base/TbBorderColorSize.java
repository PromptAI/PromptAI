package com.zervice.kbase.email.templates.config.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TbBorderColorSize {
    private String _color;
    private String _size;

    public TbBorderColorSize(TbBorderColorSize other) {
        this._color = other._color;
        this._size = other._size;
    }
}

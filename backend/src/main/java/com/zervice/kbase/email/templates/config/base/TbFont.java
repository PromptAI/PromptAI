package com.zervice.kbase.email.templates.config.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TbFont {
    private String _size;
    private String _color;

    public TbFont(TbFont other) {
        this._size = other._size;
        this._color = other._color;
    }

}

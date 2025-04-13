package com.zervice.kbase.email.templates.styling;

import lombok.Getter;

public enum FontStyle {

    NORMAL("normal"),
    ITALIC("italic"),
    OBLIQUE("oblique");

    @Getter
    private String _value;

    FontStyle(String value) {
        this._value = value;
    }
}

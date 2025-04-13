package com.zervice.kbase.email.templates.styling;

import lombok.Getter;

public enum TextDecoration {

    UNDERLINE("underline"),
    OVERLINE("overline"),
    LINE_THROUGH("line-through"),
    BLINK("blink");

    @Getter
    private String _value;

    TextDecoration(String value) {
        this._value = value;
    }
}

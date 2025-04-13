package com.zervice.kbase.email.templates.styling;

import lombok.Getter;

public enum Alignment {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right");

    @Getter
    private String _value;

    Alignment(String value) {
        this._value = value;
    }
}

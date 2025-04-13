package com.zervice.kbase.email.templates.styling;

import lombok.Getter;

public enum FontWeight {

    NORMAL("normal"),
    BOLD("bold"),
    LIGHTER("lighter");

    @Getter
    private String _value;

    FontWeight(String value) {
        this._value = value;
    }
}

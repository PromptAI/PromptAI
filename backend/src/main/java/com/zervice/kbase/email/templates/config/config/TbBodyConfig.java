package com.zervice.kbase.email.templates.config.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbBodyConfig {

    static final TbBodyConfig DEFAULT = new TbBodyConfig("#F4F4F7",
            new TbBodyBorder("1px", "#EAEAEC"),
            new TbBodyDark("#333333", "#FFF"));

    public static final TbBodyConfig newInstance() {
        return new TbBodyConfig(DEFAULT);
    }

    private String _background;
    private TbBodyBorder _border;
    private TbBodyDark _dark;

    public TbBodyConfig(TbBodyConfig other) {
        this._background = other._background;
        this._border = new TbBodyBorder(other._border);
        this._dark = new TbBodyDark(other._dark);
    }

    @Data
    @AllArgsConstructor
    public static class TbBodyBorder {
        private String _size;
        private String _color;

        public TbBodyBorder(TbBodyBorder other) {
            this._size = other._size;
            this._color = other._color;
        }
    }

    @Data
    @AllArgsConstructor
    public static class TbBodyDark {
        private String _background;
        private String _color;

        public TbBodyDark(TbBodyDark other) {
            this._background = other._background;
            this._color = other._color;
        }
    }
}

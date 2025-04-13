package com.zervice.kbase.email.templates.config.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbBoxConfig {

    static final TbBoxConfig DEFAULT = new TbBoxConfig("24px",
            "#F4F4F7",
            "2px dashed #CBCCCF",
            new TbBoxDark("#222"));

    public static final TbBoxConfig newInstance() {
        return new TbBoxConfig(DEFAULT);
    }

    private String _padding;
    private String _background;
    private String _border;
    private TbBoxDark _dark;

    public TbBoxConfig(TbBoxConfig other) {
        this._padding = other._padding;
        this._background = other._background;
        this._border = other._border;
        this._dark = new TbBoxDark(other._dark);
    }

    @Data
    @AllArgsConstructor
    public static class TbBoxDark {
        private String _background;

        public TbBoxDark(TbBoxDark other) {
            this._background = other._background;
        }
    }
}

package com.zervice.kbase.email.templates.config.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbFooterConfig {

    static final TbFooterConfig DEFAULT = new TbFooterConfig("#6B6E76",
            new TbFooterLink("#6B6E76", "underline"));

    public static final TbFooterConfig newInstance() {
        return new TbFooterConfig(DEFAULT);
    }

    private String _color;
    private TbFooterLink _link;

    public TbFooterConfig(TbFooterConfig other) {
        this._color = other._color;
        this._link = new TbFooterLink(other._link);
    }

    @Data
    @AllArgsConstructor
    public static class TbFooterLink {
        private String _color;
        private String _textDecoration;

        public TbFooterLink(TbFooterLink other) {
            this._color = other._color;
            this._textDecoration = other._textDecoration;
        }
    }
}

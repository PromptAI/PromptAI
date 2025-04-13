package com.zervice.kbase.email.templates;

import com.zervice.kbase.email.templates.model.HtmlTextEmail;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Header {

    String _text;

    String _logo;
    String _logoWidth;
    String _logoHeight;

    String _linkUrl;

    @Getter(AccessLevel.PRIVATE)
    EmailTemplateBuilder.EmailTemplateConfigBuilder _builder;

    Header(EmailTemplateBuilder.EmailTemplateConfigBuilder builder) {
        this._builder = builder;
    }

    /**
     * will replace text so that text is only alt for image
     */
    public Header logo(String logo) {
        this._logo = logo;
        return this;
    }

    /**
     * could be replaced by logo - then alt-text
     */
    public Header text(String text) {
        this._text = text;
        return this;
    }

    public Header linkUrl(String linkUrl) {
        this._linkUrl = linkUrl;
        return this;
    }

    /**
     * width in pixel
     */
    public Header logoWidth(int logoWidth) {
        this._logoWidth = String.valueOf(logoWidth);
        return this;
    }

    /**
     * height in pixel
     */
    public Header logoHeight(int logoHeight) {
        this._logoHeight = String.valueOf(logoHeight);
        return this;
    }

    /**
     * width in percentage please add %
     */
    public Header logoWidth(String logoWidth) {
        this._logoWidth = logoWidth;
        return this;
    }

    /**
     * height in percentage please add %
     */
    public Header logoHeight(String logoHeight) {
        this._logoHeight = logoHeight;
        return this;
    }

    public EmailTemplateBuilder.EmailTemplateConfigBuilder and() {
        return _builder;
    }

    public HtmlTextEmail build() {
        return _builder.build();
    }

}

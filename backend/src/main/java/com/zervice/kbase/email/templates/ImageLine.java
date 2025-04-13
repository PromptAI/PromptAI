package com.zervice.kbase.email.templates;

import com.zervice.kbase.email.templates.model.HtmlTextEmail;
import com.zervice.kbase.email.templates.styling.Alignment;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class ImageLine implements TemplateLine {

    final String _src;
    String _alt;
    String _width;
    String _height;
    String _title;
    String _linkUrl;
    String _margin;
    Alignment _alignment;

    @Getter(AccessLevel.PRIVATE)
    EmailTemplateBuilder.EmailTemplateConfigBuilder _builder;

    ImageLine(EmailTemplateBuilder.EmailTemplateConfigBuilder builder, String src) {
        this._builder = builder;

        this._src = src;
    }

    public ImageLine alt(String alt) {
        this._alt = alt;
        return this;
    }

    public ImageLine title(String title) {
        this._title = title;
        return this;
    }

    public ImageLine linkUrl(String linkUrl) {
        this._linkUrl = linkUrl;
        return this;
    }

    public ImageLine alignment(Alignment alignment) {
        this._alignment = alignment;
        return this;
    }

    public ImageLine left() {
        this._alignment = Alignment.LEFT;
        return this;
    }

    public ImageLine center() {
        this._alignment = Alignment.CENTER;
        return this;
    }

    public ImageLine right() {
        this._alignment = Alignment.RIGHT;
        return this;
    }

    /**
     * width in pixel
     */
    public ImageLine width(int width) {
        this._width = String.valueOf(width);
        return this;
    }

    /**
     * height in pixel
     */
    public ImageLine height(int height) {
        this._height = String.valueOf(height);
        return this;
    }

    /**
     * width in percentage please add %
     */
    public ImageLine width(String width) {
        this._width = width;
        return this;
    }

    /**
     * height in percentage please add %
     */
    public ImageLine height(String height) {
        this._height = height;
        return this;
    }

    /**
     * default 20px auto
     */
    public ImageLine margin(String margin) {
        this._margin = margin;
        return this;
    }

    @Override
    public TemplateLineType getType() {
        return TemplateLineType.IMAGE;
    }

    @Override
    public EmailTemplateBuilder.EmailTemplateConfigBuilder and() {
        return _builder;
    }

    @Override
    public HtmlTextEmail build() {
        return _builder.build();
    }
}

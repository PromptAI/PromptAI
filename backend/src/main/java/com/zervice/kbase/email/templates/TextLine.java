package com.zervice.kbase.email.templates;

import com.zervice.kbase.email.templates.model.HtmlTextEmail;
import com.zervice.kbase.email.templates.styling.Alignment;
import com.zervice.kbase.email.templates.styling.FontStyle;
import com.zervice.kbase.email.templates.styling.FontWeight;
import com.zervice.kbase.email.templates.styling.TextDecoration;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class TextLine implements TemplateLine {

    String _text;
    String _linkUrl;
    // style block
    Alignment _alignment;
    FontStyle _fontStyle;
    FontWeight _fontWeight;
    TextDecoration _textDecoration;
    String _color;

    @Getter(AccessLevel.PRIVATE)
    EmailTemplateBuilder.EmailTemplateConfigBuilder _builder;

    TextLine(EmailTemplateBuilder.EmailTemplateConfigBuilder builder, String text) {
        this._builder = builder;
        this._text = text;
    }

    public TextLine linkUrl(String linkUrl) {
        this._linkUrl = linkUrl;
        return this;
    }

    public TextLine alignment(Alignment alignment) {
        this._alignment = alignment;
        return this;
    }

    public TextLine center() {
        this._alignment = Alignment.CENTER;
        return this;
    }

    public TextLine right() {
        this._alignment = Alignment.RIGHT;
        return this;
    }

    public TextLine fontStyle(FontStyle fontStyle) {
        this._fontStyle = fontStyle;
        return this;
    }

    public TextLine italic() {
        this._fontStyle = FontStyle.ITALIC;
        return this;
    }

    public TextLine fontWeight(FontWeight fontWeight) {
        this._fontWeight = fontWeight;
        return this;
    }

    public TextLine bold() {
        this._fontWeight = FontWeight.BOLD;
        return this;
    }

    public TextLine lighter() {
        this._fontWeight = FontWeight.LIGHTER;
        return this;
    }

    public TextLine textDecoration(TextDecoration textDecoration) {
        this._textDecoration = textDecoration;
        return this;
    }

    public TextLine underline() {
        this._textDecoration = TextDecoration.UNDERLINE;
        return this;
    }

    public TextLine overline() {
        this._textDecoration = TextDecoration.OVERLINE;
        return this;
    }

    public TextLine lineThrough() {
        this._textDecoration = TextDecoration.LINE_THROUGH;
        return this;
    }

    /**
     * color with starting # or valid style-color lige rgb()
     */
    public TextLine color(String color) {
        this._color = color;
        return this;
    }

    @Override
    public TemplateLineType getType() {
        return TemplateLineType.TEXT;
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

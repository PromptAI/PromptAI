package com.zervice.kbase.email.templates;

import com.zervice.kbase.email.templates.model.HtmlTextEmail;
import com.zervice.kbase.email.templates.styling.Alignment;
import com.zervice.kbase.email.templates.styling.ColorPalette;
import com.zervice.kbase.email.templates.styling.ColorStyleSimple;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class ButtonLine implements TemplateLine {

    final String _text;
    final String _url;
    ColorStyleSimple _color = ColorStyleSimple.BASE_STYLE;
    Alignment _alignment = Alignment.CENTER;

    @Getter(AccessLevel.PRIVATE)
    EmailTemplateBuilder.EmailTemplateConfigBuilder _builder;

    ButtonLine(EmailTemplateBuilder.EmailTemplateConfigBuilder builder, String text, String url) {
        this._builder = builder;

        this._text = text;
        this._url = url;
    }

    public ButtonLine color(ColorStyleSimple color) {
        this._color = color;
        return this;
    }

    public ButtonLine color(ColorPalette colorPalette) {
        this._color = new ColorStyleSimple(colorPalette);
        return this;
    }

    public ButtonLine blue() {
        this._color = ColorStyleSimple.BLUE_STYLE;
        return this;
    }

    public ButtonLine green() {
        this._color = ColorStyleSimple.GREEN_STYLE;
        return this;
    }

    public ButtonLine red() {
        this._color = ColorStyleSimple.RED_STYLE;
        return this;
    }

    public ButtonLine yellow() {
        this._color = ColorStyleSimple.YELLOW_STYLE;
        return this;
    }

    public ButtonLine black() {
        this._color = ColorStyleSimple.BLACK_STYLE;
        return this;
    }

    public ButtonLine gray() {
        this._color = ColorStyleSimple.GRAY_STYLE;
        return this;
    }

    public ButtonLine alignment(Alignment alignment) {
        this._alignment = alignment;
        return this;
    }

    public ButtonLine left() {
        this._alignment = Alignment.LEFT;
        return this;
    }

    public ButtonLine center() {
        this._alignment = Alignment.CENTER;
        return this;
    }

    public ButtonLine right() {
        this._alignment = Alignment.RIGHT;
        return this;
    }

    @Override
    public TemplateLineType getType() {
        return TemplateLineType.BUTTON;
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

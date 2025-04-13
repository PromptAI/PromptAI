package com.zervice.kbase.email.templates;

import com.zervice.kbase.email.templates.model.HtmlTextEmail;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class HtmlLine implements TemplateLine {

    final String _html;
    final String _text;


    @Getter(AccessLevel.PRIVATE)
    EmailTemplateBuilder.EmailTemplateConfigBuilder _builder;

    HtmlLine(EmailTemplateBuilder.EmailTemplateConfigBuilder builder, String html, String text) {
        this._builder = builder;
        this._html = html;
        this._text = text;
    }

    @Override
    public TemplateLineType getType() {
        return TemplateLineType.HTML;
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

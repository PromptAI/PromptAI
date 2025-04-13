package com.zervice.kbase.email.templates.model;

import com.zervice.kbase.email.templates.EmailTemplateBuilder;
import com.zervice.kbase.email.templates.config.TbConfiguration;
import com.zervice.kbase.email.templates.styling.ColorStyle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HtmlTextEmail {

    private String _html;

    private String _text;

    public static void main(String[] args) throws Exception {
        TbConfiguration config = TbConfiguration.newInstance();
        config.getContent().setWidth(800);
        config.getContent().setFull(true);

        HtmlTextEmail htmlTextEmail = EmailTemplateBuilder.builder()
                .configuration(config)
                .header()
                .logo("https://www.rocketbase.io/img/logo-dark.png").logoHeight(41)
                .and()
                .text("sample-text").and()
                .text("link to google").linkUrl("https://www.google").and()
                .text("link to rocketbase").bold().underline().linkUrl("https://www.rocketbase.io").color(ColorStyle.RED).center().and()
                .image("https://cdn.rocketbase.io/assets/loading/no-image.jpg").alt("no-picture").width(300).center().and()
                .hr().margin("20px 0").and()
                .image("https://cdn.rocketbase.io/assets/signature/rocketbase-logo-signature-2020.png").alt("rocketbase").width(150).linkUrl("https://www.rocketbase.io").right().and()
                .button("click me here", "http://localhost").red().right().and()
                .button("gray is the new pink", "http://localhost").gray().left().and()
                .button("button 1", "http://adasd").and()
                .text("sample text").and()
                .attribute()
                .keyValue("KEY 1", "Value 123")
                .keyValue("KEY 2", "Value ABC")
                .and()
                .text("another text").and()
                .copyright("rocketbase").url("https://www.rocketbase.io").and()
                .footerText("my agb can be found here").linkUrl("http://localhost").and()
                .footerImage("https://cdn.rocketbase.io/assets/loading/no-image.jpg").height(50).right().and()
                .footerHr().and()
                .footerText("my little text").underline().bold().and()
                .footerImage("https://cdn.rocketbase.io/assets/loading/no-image.jpg").width(100).left().linkUrl("https://www.rocketbase.io").and()
                .build();

        System.out.println("Build Email Text:\n" + htmlTextEmail.getText());
        System.out.println("\n\n\n");
        System.out.println("Build Email HTML:\n" + htmlTextEmail.getHtml());
    }
}

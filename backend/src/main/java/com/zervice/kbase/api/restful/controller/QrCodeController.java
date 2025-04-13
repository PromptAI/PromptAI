package com.zervice.kbase.api.restful.controller;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.zervice.common.utils.DownloadUtil;
import com.zervice.kbase.api.BaseController;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author chen
 * @date 2022/11/29
 */
@RestController
@RequestMapping("/api/qrcode")
public class QrCodeController extends BaseController {

    @GetMapping
    public void generate(@RequestParam String content,
                         HttpServletResponse response,
                         @RequestParam(defaultValue = "false") Boolean icon,
                         @RequestParam(defaultValue = "200", required = false) Integer  height,
                         @RequestParam(defaultValue = "200", required = false) Integer width) throws Exception {
        QrConfig config = QrConfig.create()
                .setWidth(width)
                .setHeight(height)
                .setMargin(0);

        // logo
        if (icon) {
            Resource resource = new ClassPathResource("icon.png");
            config.setImg(resource.getFile());
        }

        File qrCode = File.createTempFile("qrcode", ".jpg");

        // generate
        BufferedImage bufferedImage = QrCodeUtil.generate(content, config);

//        // 去掉大白边
//        bufferedImage = _cropImage(bufferedImage);

        ImgUtil.write(bufferedImage, qrCode);
        DownloadUtil.download(response, qrCode);
    }

    /**
     * 原始的二维码图片无论怎么设置都是有内边距的，但是二维码是双色图，最外部的就是需要切剪掉的颜色。所以我们取到x,y 轴  = 0的第一个色值
     * 然后逐次循环到左上方码眼位置。这个码眼位置就是我们需要切割的起始位置。x,y轴循环的次数就是我们需要切割的图片的宽高的二分之一。
     *
     * 我们的二维码创建都是正方形的，这里也有好处，省略了计算不同宽高的复杂过程
     * 【描述纯属抽象，example for this !】
     */
    private static BufferedImage _cropImage(BufferedImage bufferedImage) {
        int cropColor = bufferedImage.getRGB(0, 0);
        int width = 0, height = 0;
        label:
        for (int i = 0; i < bufferedImage.getWidth(); i++) {
            for (int j = 0; j < bufferedImage.getHeight(); j++) {
                if (bufferedImage.getRGB(i, j) != cropColor) {
                    width = i;
                    height = j;
                    break label;
                }
            }
        }
        //多预留5px
        width -= 5;
        height -= 5;
        bufferedImage = bufferedImage.getSubimage(width, height,
                bufferedImage.getWidth() - (width * 2),
                bufferedImage.getHeight() - (height * 2));
        return bufferedImage;
    }
}

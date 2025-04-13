package com.zervice.common.utils;

/**
 * mask sensitive information
 */
public class MaskUtils {


    public static String maskPhoneAndIdCardAndBandCard(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return replaceBankCard(replaceIdCard(replaceCellPhone(content)));
    }


    /**
     * 验证手机号码
     * <p>
     * 移动号码段:139、138、137、136、135、134、150、151、152、157、158、159、182、183、187、188、147
     * 联通号码段:130、131、132、136、185、186、145
     * 电信号码段:133、153、180、189
     *
     * @param content
     * @return
     */
    static String replaceCellPhone(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        String regex = "((13[0-9])|(14[5|7])|(15([0-3]|[5-9]))|(18[0,5-9]))\\d{8}";
        String newContent = content;
        newContent = content.replaceAll(regex, "****");
        return newContent;
    }

    static String replaceIdCard(String content) {
        String regex = "[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]";
        if (content == null || content.isEmpty()) {
            return content;
        }
        String newContent = content;
        newContent = content.replaceAll(regex, "****");
        return newContent;
    }

    static String replaceBankCard(String content) {
        String regex = "(?<=\\d{4})\\d+(?=\\d{4})";
        if (content == null || content.isEmpty()) {
            return content;
        }
        String newContent = content;
        newContent = content.replaceAll(regex, "****");
        return newContent;
    }

    public static void main(String[] args) {
        String testMask = "这12343878是我的手机号15198141131还有我的身份证11010519491231002X你看看可以不呢?手机号15198314131和另外一个身份:11070519491231002X";
        String bankCard = "6217994730023465275";
        String x = replaceBankCard(bankCard);
//        String x = replaceIdCard(replaceCellPhone(testMask));
        System.out.println(x);
    }

}

package com.zervice.common.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class LoginUtils {
    private static final String _US_ZONE = "+1";
    private static final String _CN_ZONE = "+86";

    /**
     * Trim leading & ending spaces and convert to lower case
     *
     * @param email
     * @return
     */
    public String normalizeEmail(String email) {
        if (StringUtils.isEmpty(email)) {
            return "";
        }

        return email.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Trim leading & ending spaces, remove spaces & hypens in digits, prefix with
     * international code (default to +86)
     *
     * @param phone
     * @return
     */
    public String normalizePhone(String phone) {
        if (StringUtils.isEmpty(phone)) {
            return "";
        }

        String trimmed = phone.trim().replaceAll("\\s+|-", "");
        try {
            Long.parseLong(trimmed);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Not a valid phone number - " + trimmed);
        }

        return trimmed;
    }

    // 移动号段正则表达式
    private final Pattern pattern1 = Pattern.compile("^((13[4-9])|(147)|(15[0-2,7-9])|(178)|(18[2-4,7-8]))\\d{8}|(1705)\\d{7}$");

    // 联通号段正则表达式
    private final Pattern pattern2 = Pattern.compile("^((13[0-2])|(145)|(15[5-6])|(176)|(18[5,6]))\\d{8}|(1709)\\d{7}$");

    // 电信号段正则表达式
    private final Pattern pattern3 = Pattern.compile("^((133)|(153)|(177)|(18[0-1,9])|(149))\\d{8}$");

    // 虚拟运营商正则表达式
    private final Pattern pattern4 = Pattern.compile("^(170)\\d{8}|(1718)|(1719)\\d{7}$");

    private final Pattern patternAll = Pattern.compile("^1\\d{10}$"); // accept all # start with 1 for Chinese ...

    // private final Pattern[] patterns = new Pattern[] { pattern1, pattern2, pattern3, pattern4 };
    private final Pattern[] patterns = new Pattern[]{patternAll};

    //
    // check if it is a valid email, if company=true, we shall fail on non-company email like qq.com, gmail.com, etc.
    private static final List<String> known_suffice = Arrays.asList(
            "@gmail.com",
            "@yahoo.com",
            "@msn.com",
            "@hotmail.com",
            "@aol.com",
            "@ask.com",
            "@live.com",
            "@qq.com",
            "@vip.qq.com",
            "@foxmail.com",
            "@0355.net",
            "@163.net",
            "@263.net",
            "@yeah.net",
            "@googlemail.com",
            "@mail.com",
            "@aim.com",
            "@walla.com",
            "@inbox.com",
            "@126.com",
            "@163.com",
            "@139.com",
            "@189.cn",
            "@wo.cn",
            "@sina.com",
            "@21cn.com",
            "@sohu.com",
            "@yahoo.com.cn",
            "@tom.com",
            "@etang.com",
            "@eyou.com",
            "@56.com",
            "@x.cn",
            "@chinaren.com",
            "@sogou.com",
            "@citiz.com"
    );

    private static final Pattern emailPattern = Pattern.compile("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");
    public static boolean isValidEmail(String email) {
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        return emailPattern.matcher(trimmed).matches();
    }

    public static boolean isBusinessEmail(String email) {
        String trimmed = email.trim().toLowerCase(Locale.ROOT);

        // check suffix ...
        String domain = trimmed.substring(trimmed.indexOf('@'));

        return !known_suffice.contains(domain);
    }

    public static boolean isChineseMobile(String mobile) {
        if (mobile.startsWith("+86")) {
            mobile = mobile.substring(3);
        }

        if (mobile.length() != 11) {
            return false;
        }

        for (Pattern pattern : patterns) {
            Matcher match = pattern.matcher(mobile);
            if (match.matches()) {
                return true;
            }
        }

        return false;
    }

    public static void main(String[] args) {
        System.out.println("Is email ?" + isValidEmail("ning@zervice.us"));
        System.out.println("Is email ?" + isValidEmail("ning@zervice.us"));
        System.out.println("Is email ?" + isValidEmail("ning@41384484.us"));
        System.out.println("Is email ?" + isValidEmail("41384484@qq.us"));
        System.out.println("Is email ?" + isValidEmail("41384484@gmail.com"));
        System.out.println("Is email ?" + isValidEmail("41384484@qq.com"));

        System.out.println("Is business email ?" + isBusinessEmail("41384484@gmail.com"));
        System.out.println("Is business email ?" + isBusinessEmail("41384484@qq.com"));
        System.out.println("Is business email ?" + isBusinessEmail("41384484@zervice.com"));

        System.out.println("Phone is " + normalizePhone("18980652860"));
        System.out.println("Phone is " + normalizePhone("189 8065 2860"));
        System.out.println("Phone is " + normalizePhone("189-8065 2860"));
        System.out.println("Phone is " + normalizePhone("189-8065-2860"));
        System.out.println("Phone is " + normalizePhone("+8618980652860"));
        System.out.println("Phone is " + normalizePhone("+8618980652860"));
        System.out.println("Phone is " + normalizePhone("17308039592"));

        String phone = "";
        try {
            phone = "189--8065 2860";
            normalizeEmail(phone);
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid number " + phone);
        }

        try {
            phone = "189/8065 2860";
            System.out.println("Phone is " + normalizePhone(phone));
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid number " + phone);
        }

        try {
            phone = "189s80652860";
            System.out.println("Phone is " + normalizePhone(phone));
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid number " + phone);
        }
    }
}

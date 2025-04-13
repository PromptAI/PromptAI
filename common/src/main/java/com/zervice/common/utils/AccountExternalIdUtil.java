package com.zervice.common.utils;

import com.zervice.common.filter.FilterConfigName;
import com.zervice.common.filter.MutableHttpServletRequest;
import com.zervice.common.pojo.common.Account;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Peng Chen
 * @date 2020/3/27
 */
@Log4j2
public class AccountExternalIdUtil {

    /**
     * return accountId if exist;
     * return null if accountName is blank or account not fount
     */
    public static String getAccountInRequest(MutableHttpServletRequest httpRequest, AccountService accountService) {
        //如果有accountId，直接获取
        String accountId = httpRequest.getHeader(FilterConfigName.X_EXTERNAL_ACCOUNT_ID_HEADER);
        if (StringUtils.isNotBlank(accountId)) {
            return accountId;
        }

        String accountName = httpRequest.getHeader(FilterConfigName.X_EXTERNAL_ACCOUNT_NAME_HEADER);
        if (!StringUtils.isEmpty(accountName)) {
            //获取accountId
            return _getAccountExternalIdFromAccountName(accountName, accountService);
        }


        //获取host
        String host = httpRequest.getHeader(FilterConfigName.HEADER_HOST);
        //host不是ip,则获取name
        accountName = getAccountNameFromHost(host);

        //获取accountId
        return _getAccountExternalIdFromAccountName(accountName, accountService);
    }


    private static String _getAccountExternalIdFromAccountName(String accountName, AccountService accountService) {
        if (StringUtils.isBlank(accountName)) {
            LOG.error("[{}][account not found  accountName is blank.accountName:{}]", Constants.COREDB, accountName);
            return null;
        }

        Account account = accountService.getByName(accountName);
        if (account == null) {
            LOG.error("[{}]account not found  form  db, accountName:{}", Constants.COREDB, accountName);
            return null;
        }

        return account.getExternalId();
    }

    public static String getAccountNameFromHost(String host) {
        // 根据url host domain 获取
        int firstDot = host.indexOf('.');
        if (firstDot > 0) {
            return host.substring(0, firstDot);
        }

        LOG.error("account name not found from  host {}", host);
        return null;
    }
}

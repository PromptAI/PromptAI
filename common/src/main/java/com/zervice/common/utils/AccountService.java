package com.zervice.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.zervice.common.model.AccountModel;
import com.zervice.common.pojo.common.Account;

import java.sql.Connection;
import java.util.List;

/**
 * account service
 */
public interface AccountService {

    /**
     * 获取所有account
     *
     * @return accounts
     */
    List<Account> getAll();

    /**
     * 通过account.is获取account
     *
     * @param acctExternalId account.id
     * @return account
     */
    Account get(String acctExternalId);

    /**
     * 创建account
     *
     * @param account data
     * @return o
     */
    Account create(AccountModel account) throws Exception;

    /**
     * 删除
     *
     * @param acctExternalId account.id
     */
    void delete(String acctExternalId);

    /**
     * 根据名称获取account
     *
     * @param accountName
     * @return NULL/account
     */
    Account getByName(String accountName);

    Account update(String accountExtId, AccountModel account);

    Account updateStatus(JSONObject data) throws Exception;

    Boolean enoughToken(Connection conn, String dbName) throws Exception;

    /**
     * 检查token是否充足，扔出一个标准的异常；
     *  如果不想使用这个异常提示，请调用{@link AccountService#enoughToken(Connection, String)} 进行检查后自定义处理
     *
     *  如果是用户配置的OpenAI key是无需检查token的。
     * @param conn
     * @param dbName
     * @throws Exception
     */
    void checkEnoughToken(Connection conn, String dbName) throws Exception;
}
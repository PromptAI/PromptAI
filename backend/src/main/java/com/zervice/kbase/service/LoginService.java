package com.zervice.kbase.service;

import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.api.restful.pojo.RestUser;
import com.zervice.kbase.database.pojo.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.sql.Connection;

/**
 * login/logout
 */
public interface LoginService {

    /**
     * login
     *
     * @param request      http request
     * @return user.name
     */
    Object login(HttpServletRequest request) throws Exception;

    /**
     * logout
     *
     * @param httpSession session
     * @return LoginResponse.empty
     */
    EmptyResponse logout(HttpSession httpSession);

    /**
     * get user info
     *
     * @param id     user id
     * @param dbName database name
     * @return user info
     */
    RestUser info(long id, String dbName) throws Exception;


    Object refreshToken(HttpServletRequest request, long userId, String dbName) throws Exception;

    Object loginWithUser(User signInUser, AccountCatalog account, Connection conn) throws Exception;
}

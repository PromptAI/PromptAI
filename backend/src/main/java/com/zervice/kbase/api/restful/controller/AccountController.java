package com.zervice.kbase.api.restful.controller;

import com.zervice.common.model.AccountModel;
import com.zervice.common.pojo.common.Account;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.utils.AccountService;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.pojo.PageResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FIXME:
 *    Move to separte account admin application
 *    For now, let's connect with AccountCatalog ...
 */
@Log4j2
@RestController
@RequestMapping("/api/accounts")
public class AccountController extends BaseController {

    @Autowired
    private AccountService accountService;

    @PostMapping("init")
    public Object init(@RequestBody AccountModel accountInfo) throws Exception{
        accountService.create(accountInfo);
        return EmptyResponse.empty();
    }

    @GetMapping
    public PageResponse<Account> getAll() {

        //query
        List<Account> accounts = accountService.getAll();

        return PageResponse.of(accounts);
    }

    @GetMapping("{id}")
    public Account get(@PathVariable("id") String acctExternalId) {

        //query
        Account account = accountService.get(acctExternalId);

        return account;
    }
}

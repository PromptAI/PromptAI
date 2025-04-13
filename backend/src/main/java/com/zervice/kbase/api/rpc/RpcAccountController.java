package com.zervice.kbase.api.rpc;

import com.zervice.common.i18n.MessageUtils;
import com.zervice.common.pojo.common.EmptyResponse;
import com.zervice.common.utils.Maps;
import com.zervice.kbase.api.BaseController;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.llm.LlmServiceHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenchen
 * @Date 2023/8/18
 */
@Log4j2
@RestController
@RequestMapping("/rpc/account")
public class RpcAccountController extends BaseController {
    @GetMapping("/enough/token")
    public Object enoughToken(@RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName) throws Exception {
        Boolean enoughToken = LlmServiceHelper.enoughToken(dbName);
        if (enoughToken) {
            return EmptyResponse.empty();
        }

        return Maps.of("error", MessageUtils.tokenNotEnoughErrorMessage());
    }
}

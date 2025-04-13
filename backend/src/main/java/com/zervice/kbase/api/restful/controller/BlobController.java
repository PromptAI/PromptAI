package com.zervice.kbase.api.restful.controller;

import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.Constants;
import com.zervice.common.utils.NetworkUtils;
import com.zervice.kbase.api.restful.AuthFilter;
import com.zervice.kbase.api.restful.pojo.RestBlob;
import com.zervice.kbase.database.dao.CommonBlobDao;
import com.zervice.kbase.database.dao.ConfigurationDao;
import com.zervice.kbase.database.pojo.CommonBlob;
import com.zervice.kbase.database.pojo.Configuration;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import com.zervice.kbase.service.BlobService;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

@Log4j2
@RequestMapping(value = "/api/blobs/")
@RestController
public class BlobController {

    private final BlobService blobService = BlobService.getInstance();

    @GetMapping("get/{id}")
    public ResponseEntity<byte[]> getBlob(@PathVariable(name = "id") String externalFileId) throws SQLException {
        String dbName = blobService.parseDbNameFromId(externalFileId);
        Long id = CommonBlob.idFromExternalId(externalFileId);
        if (id == null) {
            LOG.warn("file not found:{}", externalFileId);
            throw new RestException(StatusCodes.FileNotFound);
        }
        return getBlob(id, dbName);
    }

    public static ResponseEntity<byte[]> getBlob(long blobId,
                                                 String dbName) throws SQLException {

        @Cleanup Connection conn = DaoUtils.getConnection(true);
        CommonBlob blob = CommonBlobDao.getById(conn, dbName, blobId);
        if (blob == null) {
            LOG.error("[{}]blob not exit.id:{}", dbName, blobId);
            throw new RestException(StatusCodes.FileNotFound);
        }
        //set header
        HttpHeaders httpHeader = new HttpHeaders();
        httpHeader.setContentDispositionFormData("attachment", new String(blob.getFileName().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
        httpHeader.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeader.setCacheControl("max-age=315360000");
        return new ResponseEntity<>(blob.getContent(), httpHeader, HttpStatus.OK);
    }

    @PostMapping("upload")
    public RestBlob upload(@RequestParam("file") MultipartFile multipartFile,
                           @RequestParam(value = "type", defaultValue = "1") Integer type,
                           HttpServletRequest request,
                           @RequestHeader(AuthFilter.X_EXTERNAL_ACCOUNT_ID_HEADER) String dbName,
                           @RequestHeader(AuthFilter.X_EXTERNAL_USER_ID_HEADER) String uid) throws Exception {
        long userId = User.fromExternalId(uid);
        String ip = NetworkUtils.getRemoteIP(request);
        return blobService.upload(multipartFile, type, userId, ip, dbName);
    }

    @GetMapping("group/qrcode")
    public Object getQrcodeUri(@RequestParam("type") String type) throws SQLException {

        String configKey = CommonBlob.Qrcode2ConfigurationKey.get(type);
        if (StringUtils.isBlank(configKey)) {
            LOG.error("[{}][not support qrcode type,type:{}]", Constants.COREDB, type);
            throw new RestException(StatusCodes.UNSUPPORTED_QRCODE_TYPE);
        }
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        Configuration conf = ConfigurationDao.getByName(conn, Constants.COREDB, configKey);
        if (conf == null) {
            LOG.error("[{}][configuration name not exist.configuration name:{}]", Constants.COREDB, configKey);
            throw new RestException(StatusCodes.FileNotFound);
        }
        return getBlob(conf.getValue());
    }
}

package com.zervice.kbase.service;

import com.zervice.common.pojo.common.Account;
import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import com.zervice.common.utils.IdGenerator;
import com.zervice.common.utils.LayeredConf;
import com.zervice.common.utils.ServletUtils;
import com.zervice.kbase.AccountCatalog;
import com.zervice.kbase.ZBotRuntime;
import com.zervice.kbase.api.restful.pojo.RestBlob;
import com.zervice.kbase.database.dao.AuditLogDao;
import com.zervice.kbase.database.dao.CommonBlobDao;
import com.zervice.kbase.database.dao.UserDao;
import com.zervice.kbase.database.pojo.AuditLog;
import com.zervice.kbase.database.pojo.CommonBlob;
import com.zervice.kbase.database.pojo.User;
import com.zervice.kbase.database.utils.DaoUtils;
import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author chen
 * @date 2022/8/30
 */
@Log4j2
public class BlobService {

    private static List<String> allowedFileTypes;

    static {
        allowedFileTypes = Arrays.asList(LayeredConf.getStringArray("allowed.upload.file.types"));
        if (CollectionUtils.isEmpty(allowedFileTypes)) {
            //.doc,.docx,.xls,.xlsx,.ppt,.pptx,.pdf,.zip,.rar,.7z,.tar,.tar.gz
            allowedFileTypes = Arrays.asList(".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
                    ".pdf", ".zip", ".rar", ".7z", ".tar", ".tar.gz","txt");
        }
    }

    private static long uploadFileLimitSize = LayeredConf.getLong("upload.file.limit.size", 20 * 1024 * 1024);

    private BlobService() {
    }


    private static final BlobService _instance = new BlobService();

    public static BlobService getInstance() {
        return _instance;
    }

    public RestBlob upload(MultipartFile multipartFile, Integer type,
                           long userId, String ip, String dbName) throws Exception {
        AccountCatalog.ensure(dbName);
        if (multipartFile.getSize() > uploadFileLimitSize) {
            throw new RestException(StatusCodes.FILE_IS_TOO_LARGE);
        }

        @Cleanup Connection conn = DaoUtils.getConnection(false);

        String md5 = DigestUtils.md5DigestAsHex(multipartFile.getInputStream());
        // give me an id
        long blobId = IdGenerator.generateId();

        //query
        User dbUser = UserDao.get(conn, dbName, userId);
        if (dbUser == null) {
            throw new RestException(StatusCodes.NotFound);
        }

        Pair<String, byte[]> fileNameAndContent = _pairFile(multipartFile);

        CommonBlob.Prop prop = CommonBlob.Prop.builder()
                .ip(ip).createTime(System.currentTimeMillis())
                .build();

        CommonBlob blob = CommonBlob.builder()
                .id(blobId)
                .fileName(fileNameAndContent.getLeft())
                .type(type)
                .content(fileNameAndContent.getRight())
                .md5(md5).properties(prop)
                .build();

        CommonBlobDao.add(conn, dbName, blob);

        conn.commit();
        return new RestBlob(dbName, blob);
    }

    public RestBlob save(InputStream inputStream, String originName,
                         String ip, Integer type,
                         Connection conn, String dbName) throws Exception{
        byte[] data = _readBytesFromInputStream(inputStream);
        // give me an id
        long blobId = IdGenerator.generateId();

        CommonBlob.Prop prop = CommonBlob.Prop.builder()
                .ip(ip).createTime(System.currentTimeMillis())
                .build();

        CommonBlob blob = CommonBlob.builder().id(blobId)
                .fileName(originName)
                .type(type)
                .content(data)
                .md5(DigestUtils.md5DigestAsHex(data))
                .properties(prop)
                .build();

        CommonBlobDao.add(conn, dbName, blob);

        return new RestBlob(dbName, blob);
    }

    public RestBlob save(File file, Connection conn, String dbName) throws Exception {
        @Cleanup FileInputStream fileInputStream = new FileInputStream(file);
        return save(fileInputStream, file.getName(), null, CommonBlob.TYPE_USER_AVATAR, conn, dbName);
    }

    private byte[] _readBytesFromInputStream(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //write file
        try {
            IOUtils.copy(inputStream, byteArrayOutputStream);
        } catch (IOException e) {
            throw new RestException(StatusCodes.InternalError, "write file error:" + e);
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * fileName, content
     */
    private Pair<String, byte[]> _pairFile(MultipartFile multipartFile) throws Exception {
        if (multipartFile == null) {
            return null;
        }
        String originalFileName = multipartFile.getOriginalFilename();
        byte[] data = _readBytesFromInputStream(multipartFile.getInputStream());

        return ImmutablePair.of(originalFileName, data);
    }

    /**
     * 通过id获取数据库文件存储对象
     * externalFileId:a1_cetu2yr2kwlc
     */
    public CommonBlob getBlob(String externalFileId,String dbName) throws Exception {
        Long blobId = CommonBlob.idFromExternalId(externalFileId);
        @Cleanup Connection conn = DaoUtils.getConnection(true);
        CommonBlob blob = CommonBlobDao.getById(conn, dbName, blobId);
        if (blob == null) {
            LOG.error("[{}]blob not exit.id:{}", dbName, blobId);
            throw new RestException(StatusCodes.FileNotFound);
        }
        return blob;
    }

    public String parseDbNameFromId(String externalFileId) {
        try {
            return CommonBlob.dbNameFromExternalId(externalFileId);
        } catch (Exception e) {
            return Account.getAccountDbName(ZBotRuntime.STANDALONE_ACCOUNT_ID);
        }
    }

    /**
     * 通过id删除文件
     *externalFileId:a1_ceuhjp8q0dts
     */
    public void deleteByExtId(String externalFileId, Connection conn) throws Exception {
        String fileDbName = parseDbNameFromId(externalFileId);
        Long fileId = CommonBlob.idFromExternalId(externalFileId);
        CommonBlobDao.delete(conn, fileDbName, fileId);
    }

}

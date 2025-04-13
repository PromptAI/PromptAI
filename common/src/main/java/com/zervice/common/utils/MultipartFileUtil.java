package com.zervice.common.utils;

import com.zervice.common.restful.exception.RestException;
import com.zervice.common.restful.exception.StatusCodes;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @author Peng Chen
 * @date 2022/6/30
 */
@Log4j2
public class MultipartFileUtil {

    public  static File toFile(String path, MultipartFile file) {
        File dest = new File(path);

        if (file.getSize() <= 0 || StringUtils.isBlank(file.getName()) || StringUtils.isBlank(file.getOriginalFilename())) {
            throw new RestException(StatusCodes.BadRequest, "invalid file");
        }

        try {
            file.transferTo(dest);
            return dest;
        } catch (Exception e) {
            LOG.error("save train data to:{} fail:{} ", path, e.getMessage(), e);
            throw new RestException(StatusCodes.InternalError, "internal error");
        }
    }
}

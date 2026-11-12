package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/admin/common")
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        log.info("文件上传 : {}", file);

        String originalFilename = file.getOriginalFilename();//获取原始文件名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));//获取文件后缀
        String objectName = UUID.randomUUID().toString() + suffix;//拼接上传的文件名

        try {
            String upload = aliOssUtil.upload(file.getBytes(), objectName);//上传文件，获取上传后图片的地址
            return Result.success(upload);
        } catch (IOException e) {
            log.error("文件上传失败:{}", e);
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }


    }
}

package com.liudaolunhuibl.plugin.pojo;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.util.List;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: JavaFile
 * @Description: java文件对象
 * @date 2023/6/20
 */
@Data
@Builder
public class JavaFile {

    /**
     * Java文件包名
     */
    private String packageName;

    /**
     * java文件类名
     */
    private String className;

    /**
     * java代码，一行是一个元素，没有空行
     */
    List<String> codeLines;

    /**
     * java文件绝对路径
     */
    private String absolutePath;

}

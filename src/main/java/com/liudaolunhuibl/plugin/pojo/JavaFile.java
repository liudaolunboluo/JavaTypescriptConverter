package com.liudaolunhuibl.plugin.pojo;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

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
     * 类作者
     */
    private String classAuthor;

    /**
     * 类创建日期
     */
    private String classCreateDate;

    /**
     * 字段信息
     */
    List<JavaFieldInfo> fieldInfos;

    /**
     * java文件绝对路径
     */
    private String absolutePath;

}

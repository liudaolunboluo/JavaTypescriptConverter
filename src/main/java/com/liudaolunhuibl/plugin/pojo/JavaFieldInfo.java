package com.liudaolunhuibl.plugin.pojo;

import com.github.javaparser.ast.type.Type;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: JavaFieldInfo
 * @Description: java pojo字段信息
 * @date 2023/8/23
 */
@Data
@Builder
public class JavaFieldInfo {

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 字段类型
     */
    private String fieldType;

    /**
     * 字段注释
     */
    private String comment;

    /**
     * map字段类型
     */
    private Pair<String, String> mapGenericType;

    /**
     * 集合字段类型
     */
    private String collGenericType;

    /**
     * 是否是集合
     */
    private Boolean isCollection;

    /**
     * 是否是Map
     */
    private Boolean isMap;
}

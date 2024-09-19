package com.liudaolunhuibl.plugin.core.pojo;

import lombok.Data;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: StaticClassTestPojo
 * @Description: 静态类测试
 * @date 2024/9/18
 */
@Data
public class StaticClassTestPojo {

    private String name;

    private Integer age;

    private InnerClass innerClass;

    @Data
    static class InnerClass {

        private Long id;

        private String info;
    }
}

package com.liudaolunhuibl.plugin.core;

import com.google.inject.internal.util.Lists;
import org.junit.jupiter.api.Test;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: JavaPojoToTypeScriptConverterTest
 * @Description: JavaPojoToTypeScriptConverter 测试
 * @date 2024/9/18
 */
public class JavaPojoToTypeScriptConverterTest {

    @Test
    public void testStaticClass() {
        String path = System.getProperty("user.dir") + "/src/test/java";
        final JavaPojoToTypeScriptConverter converter = JavaPojoToTypeScriptConverter.builder()
                .targetPackageList(Lists.newArrayList("com.liudaolunhuibl.plugin.core.pojo")).sourceDirectory(path).targetDirectory(path + "/typescript")
                .build();
        converter.convert();

    }
}

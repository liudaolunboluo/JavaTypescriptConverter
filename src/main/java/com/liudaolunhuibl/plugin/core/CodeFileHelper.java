package com.liudaolunhuibl.plugin.core;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: CodeFileUtils
 * @Description: 代码文件帮助类
 * @date 2023/6/23
 */
@UtilityClass
public class CodeFileHelper {

    /**
     * 获取目标包下的所有java文件
     *
     * @param sourcePath：源码根目录
     * @param packageList:配置的包名集合
     * @author zhangyunfan
     * @date 2023/6/23
     */
    public List<File> getTargetPackageFile(String sourcePath, List<String> packageList) {
        List<File> result = new ArrayList<>();
        Path basePath = Paths.get(sourcePath);
        for (String packageName : packageList) {
            String packagePath = packageName.replace(".", "/");
            String fullPath = basePath + "/" + packagePath;
            File packageDir = new File(fullPath);
            if (packageDir.exists() && packageDir.isDirectory()) {
                collectFiles(packageDir, result);
            }
        }
        return result;
    }

    /**
     * 递归获取目录下的所有java文件，包含子目录下的java文件
     *
     * @param targetDirectory:目标目录
     * @param files:文件结果集合
     * @author zhangyunfan
     * @date 2023/6/23
     */
    private void collectFiles(File targetDirectory, List<File> files) {
        if (targetDirectory.isDirectory()) {
            for (File file : Objects.requireNonNull(targetDirectory.listFiles())) {
                if (file.isDirectory()) {
                    collectFiles(file, files);
                } else if (file.getName().endsWith(".java")) {
                    files.add(file);
                }
            }
        }
    }

}

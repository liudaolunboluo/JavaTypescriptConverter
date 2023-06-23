package com.liudaolunhuibl.plugin.helper;

import com.liudaolunhuibl.plugin.pojo.JavaFile;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
     * java代码统一前缀
     */
    private static final String COMMON_SRC_PATH = "/src/main/java/";

    /**
     * 根据代码文件生成java文件对象，包含类型、包名以及代码
     *
     * @param codeFile:代码文件对象
     * @return java文件对象，包含类型、包名以及代码
     * @author zhangyunfan
     * @date 2023/6/23
     */
    public JavaFile buildJavaFileByCodeFile(File codeFile) {
        final List<String> codeLines = CodeFileHelper.readFileLines(codeFile);
        if (codeLines.isEmpty()) {
            return null;
        }
        String absolutePath = codeFile.getAbsolutePath();
        String basePath = absolutePath.substring(absolutePath.lastIndexOf(COMMON_SRC_PATH) + COMMON_SRC_PATH.length());
        final String[] pathArr = basePath.split("/");
        String className = pathArr[pathArr.length - 1];
        String packageName = basePath.replace("/" + className, "").replace("/", ".");
        return JavaFile.builder().className(className.replace(".java", "")).packageName(packageName).absolutePath(codeFile.getAbsolutePath())
                .codeLines(codeLines).build();
    }

    /**
     * 获取文件内容不包含空行
     *
     * @param file:文件
     * @return 文件内容不包含空行
     * @author zhangyunfan
     * @date 2023/6/23
     */
    public List<String> readFileLines(File file) {
        List<String> lines;
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
        List<String> nonEmptyLines = new ArrayList<>();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                nonEmptyLines.add(line);
            }
        }
        return nonEmptyLines;
    }

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

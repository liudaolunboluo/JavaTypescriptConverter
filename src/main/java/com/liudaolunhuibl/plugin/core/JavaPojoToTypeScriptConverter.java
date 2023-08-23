package com.liudaolunhuibl.plugin.core;

import com.liudaolunhuibl.plugin.context.LogContext;
import com.liudaolunhuibl.plugin.pojo.JavaFieldInfo;
import com.liudaolunhuibl.plugin.pojo.JavaFile;
import lombok.Builder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: JavaToTypeScriptConverter
 * @Description: java pojo类转换为ts文件
 * @date 2023/8/23
 */
@Builder
public class JavaPojoToTypeScriptConverter {

    private List<String> targetPackageList;

    private String sourceDirectory;

    private String targetDirectory;

    private static final String BODY_DISTORTION_SYMBOL = StringUtils.repeat(StringUtils.SPACE, 3);

    public void convert() {
        //找到配置的包下的java文件
        List<JavaFile> targetJavaFile = findJavaFilesFromTarget(this.targetPackageList, this.sourceDirectory);
        if (targetJavaFile.isEmpty()) {
            LogContext.getLog().error("config package:[" + StringUtils.join(this.targetPackageList, ",") + "] is all has no code,plz check it");
            return;
        }
        //根据java文件生成typescript文件
        generateTypescriptFile(this.targetDirectory, targetJavaFile);
        LogContext.getLog().info("success to convert java to typescript!,plz watch it to ：" + this.targetDirectory);
    }

    private void generateTypescriptFile(String targetDirectory, List<JavaFile> targetJavaFiles) {
        for (JavaFile targetFile : targetJavaFiles) {
            this.generateTypescriptFile(targetDirectory, targetFile);
        }
    }

    private void generateTypescriptFile(String targetDirectory, JavaFile targetFile) {
        final List<String> typescriptCode = convert2typescript(targetFile);
        try {
            final String targetFileName =
                    targetDirectory + "/" + targetFile.getPackageName().replace(".", "/") + "/" + targetFile.getClassName() + ".ts";
            FileUtils.writeStringToFile(new File(targetFileName), String.join("\n", typescriptCode), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogContext.getLog().error(targetFile.getAbsolutePath() + "file convert fail", e);
        }
    }

    public List<JavaFile> findJavaFilesFromTarget(List<String> packageList, String sourceDirectory) {
        List<File> codeFiles = CodeFileHelper.getTargetPackageFile(sourceDirectory, packageList);
        if (codeFiles.isEmpty()) {
            LogContext.getLog().error("config package:[" + StringUtils.join(packageList, ",") + "] is all empty,plz check it");
            return new ArrayList<>();
        }
        List<JavaFile> targetJavaFile = new ArrayList<>(codeFiles.size());
        for (File c : codeFiles) {
            //替换为ast语法树来解析而不是解析字符串
            final JavaFile javaFile = JavaSourceCodeAnalyzer.generateJavaFileFromFile(c);
            if (javaFile == null) {
                LogContext.getLog().error("targetFile:[" + c.getAbsolutePath() + "] is empty code file");
                continue;
            }
            targetJavaFile.add(javaFile);
        }
        return targetJavaFile;
    }

    private List<String> convert2typescript(JavaFile javaFile) {
        List<String> typescriptLines = new ArrayList<>(javaFile.getFieldInfos().size());
        typescriptLines.add("/**");
        typescriptLines.add(" * @author " + javaFile.getClassAuthor());
        typescriptLines.add(" * @date " + javaFile.getClassCreateDate());
        typescriptLines.add("*/");
        //类声名
        typescriptLines.add("class " + javaFile.getClassName() + " {");
        //方法体代码
        for (JavaFieldInfo fieldInfo : javaFile.getFieldInfos()) {
            if (StringUtils.isNotBlank(fieldInfo.getComment())) {
                typescriptLines.add(BODY_DISTORTION_SYMBOL + "/**");
                typescriptLines.add(BODY_DISTORTION_SYMBOL + " * " + fieldInfo.getComment());
                typescriptLines.add(BODY_DISTORTION_SYMBOL + "*/");
                StringBuilder sb = new StringBuilder();
                sb.append(BODY_DISTORTION_SYMBOL).append(fieldInfo.getFieldName()).append(": ");
                if (Boolean.TRUE.equals(fieldInfo.getIsCollection())) {
                    sb.append(JavaTypeMapping.javaTypeConvertTypescriptType(fieldInfo.getCollGenericType()));
                    sb.append("[]");
                } else if (Boolean.TRUE.equals(fieldInfo.getIsMap())) {
                    sb.append("Map<").append(JavaTypeMapping.javaTypeConvertTypescriptType(fieldInfo.getMapGenericType().getLeft())).append(", ")
                            .append(JavaTypeMapping.javaTypeConvertTypescriptType(fieldInfo.getMapGenericType().getRight())).append(">");
                } else {
                    sb.append(JavaTypeMapping.javaTypeConvertTypescriptType(fieldInfo.getFieldType()));
                }
                typescriptLines.add(sb.toString());
            }
        }
        typescriptLines.add("}");
        typescriptLines.add("export default " + javaFile.getClassName() + ";");
        return typescriptLines;

    }
}

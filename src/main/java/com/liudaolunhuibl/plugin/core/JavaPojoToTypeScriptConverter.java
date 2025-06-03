package com.liudaolunhuibl.plugin.core;

import com.google.inject.internal.util.Lists;
import com.liudaolunhuibl.plugin.context.LogContext;
import com.liudaolunhuibl.plugin.enums.TypescriptModeEnum;
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
import java.util.Map;
import java.util.stream.Collectors;

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

    private String typescriptMode;

    private static final String BODY_DISTORTION_SYMBOL = StringUtils.repeat(StringUtils.SPACE, 3);

    private static final String LINE_BREAKS = "\n";

    private static final String FILE_SUFFIX = ".ts";

    public void convert() {
        //找到配置的包下的java文件
        List<JavaFile> targetJavaFile = findJavaFilesFromTarget(this.targetPackageList, this.sourceDirectory);
        if (targetJavaFile == null || targetJavaFile.isEmpty()) {
            LogContext.error("config package:[" + StringUtils.join(this.targetPackageList, ",") + "] is all has no code,plz check it");
            return;
        }

        if (TypescriptModeEnum.CLASS_MODEL.getMode().equals(typescriptMode)) {
            //根据java文件生成typescript文件,这种模式是一个类一个文件，并且是class
            generateTypescriptClassFile(this.targetDirectory, targetJavaFile);
        } else if (TypescriptModeEnum.INTERFACE_MODEL.getMode().equals(typescriptMode)) {
            //根据java文件生成typescript文件,这种模式是一个java包在一个ts文件，并且是interface
            generateTypescriptInterfaceFile(this.targetDirectory, targetJavaFile);
        } else {
            throw new IllegalArgumentException("typescriptMode must be either 'class' or 'interface'.");
        }
        LogContext.info("success to convert java to typescript!,plz watch it to ：" + this.targetDirectory);
    }

    private void generateTypescriptClassFile(String targetDirectory, List<JavaFile> targetJavaFiles) {
        targetJavaFiles.forEach(targetFile -> this.generateTypescriptClassFile(targetDirectory, targetFile));
    }

    private void generateTypescriptInterfaceFile(String targetDirectory, List<JavaFile> targetJavaFiles) {
        //按照package分组，一个package一个ts文件
        Map<String, List<JavaFile>> groupedFiles = targetJavaFiles.stream().collect(Collectors.groupingBy(JavaFile::getPackageName));
        groupedFiles.forEach((packageName, javaFiles) -> generateTypescriptInterfaceCode(packageName, javaFiles, targetDirectory));

    }

    private void generateTypescriptClassFile(String targetDirectory, JavaFile targetFile) {
        final List<String> typescriptCode = convert2typescript(targetFile, TypescriptModeEnum.CLASS_MODEL);
        try {
            final String targetFileName =
                    targetDirectory + File.separator + targetFile.getPackageName().replace(".", "/") + File.separator + targetFile.getClassName()
                            + FILE_SUFFIX;
            FileUtils.writeStringToFile(new File(targetFileName), String.join(LINE_BREAKS, typescriptCode), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogContext.error(targetFile.getAbsolutePath() + "file convert fail", e);
        }
    }

    private void generateTypescriptInterfaceCode(String packageName, List<JavaFile> targetJavaFiles, String targetDirectory) {
        List<String> interfaceCode = Lists.newArrayList();
        for (JavaFile targetFile : targetJavaFiles) {
            interfaceCode.addAll(convert2typescript(targetFile, TypescriptModeEnum.INTERFACE_MODEL));
            interfaceCode.add(StringUtils.EMPTY);
        }
        try {
            final String targetFileName =
                    targetDirectory + File.separator + packageName.replace(".", "/") + File.separator + getPackageSimpleName(packageName)
                            + FILE_SUFFIX;
            FileUtils.writeStringToFile(new File(targetFileName), String.join(LINE_BREAKS, interfaceCode), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogContext.error("package:" + packageName + " convert fail", e);
        }
    }

    public static String getPackageSimpleName(String packageName) {
        final String[] packageNameArr = packageName.split("\\.");
        return packageNameArr[packageNameArr.length - 1];
    }

    public List<JavaFile> findJavaFilesFromTarget(List<String> packageList, String sourceDirectory) {
        List<File> codeFiles = CodeFileHelper.getTargetPackageFile(sourceDirectory, packageList);
        if (codeFiles == null || codeFiles.isEmpty()) {
            LogContext.error("config package:[" + StringUtils.join(packageList, ",") + "] is all empty,plz check it");
            return new ArrayList<>();
        }
        List<JavaFile> targetJavaFile = new ArrayList<>(codeFiles.size());
        for (File c : codeFiles) {
            //替换为ast语法树来解析而不是解析字符串
            //这里是List的原因是可能存在static class内部类
            List<JavaFile> javaFiles = JavaSourceCodeAnalyzer.generateJavaFileFromFile(c);
            if (javaFiles == null || javaFiles.isEmpty()) {
                LogContext.error("targetFile:[" + c.getAbsolutePath() + "] is empty code file");
                // continue;
                throw new IllegalArgumentException("targetFile:[" + c.getAbsolutePath() + "] analyze fail!perhaps it is empty file!check it plz");
            }
            targetJavaFile.addAll(javaFiles);
        }
        return targetJavaFile;
    }

    private List<String> convert2typescript(JavaFile javaFile, TypescriptModeEnum modeEnum) {
        List<String> typescriptLines = new ArrayList<>(javaFile.getFieldInfos().size());
        //只有class模式也就是一个class一个ts需要去import内部类，interface模式不需要
        if (javaFile.getInnerClass() != null && !javaFile.getInnerClass().isEmpty() && TypescriptModeEnum.CLASS_MODEL.equals(modeEnum)) {
            for (String innerClass : javaFile.getInnerClass()) {
                typescriptLines.add("import " + innerClass + " from " + "\"./" + innerClass + "\"");
            }
        }
        typescriptLines.add("/**");
        typescriptLines.add(" * @author " + javaFile.getClassAuthor());
        typescriptLines.add(" * @date " + javaFile.getClassCreateDate());
        typescriptLines.add("*/");
        if (TypescriptModeEnum.CLASS_MODEL.equals(modeEnum)) {
            //类声名
            typescriptLines.add("class " + javaFile.getClassName() + " {");
        } else if (TypescriptModeEnum.INTERFACE_MODEL.equals(modeEnum)) {
            typescriptLines.add("export interface " + javaFile.getClassName() + " {");
        } else {
            throw new IllegalArgumentException("typescriptMode must be either 'class' or 'interface'.");
        }

        //方法体代码
        for (JavaFieldInfo fieldInfo : javaFile.getFieldInfos()) {
            if (StringUtils.isNotBlank(fieldInfo.getComment())) {
                typescriptLines.add(BODY_DISTORTION_SYMBOL + "/**");
                typescriptLines.add(BODY_DISTORTION_SYMBOL + " * " + fieldInfo.getComment());
                typescriptLines.add(BODY_DISTORTION_SYMBOL + "*/");
            }
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
        typescriptLines.add("}");
        if (TypescriptModeEnum.CLASS_MODEL.equals(modeEnum)) {
            typescriptLines.add("export default " + javaFile.getClassName() + ";");
        }
        return typescriptLines;

    }
}

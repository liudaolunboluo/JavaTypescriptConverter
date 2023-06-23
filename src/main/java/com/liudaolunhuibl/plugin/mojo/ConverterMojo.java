package com.liudaolunhuibl.plugin.mojo;

import com.liudaolunhuibl.plugin.pojo.JavaFile;
import com.liudaolunhuibl.plugin.util.CodeFileUtils;
import com.liudaolunhuibl.plugin.util.JavaTypeUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: ConverterMojo
 * @Description: 转换类
 * @date 2023/6/20
 */
@Mojo(name = "TypescriptConverter")
public class ConverterMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    public MavenProject project;

    @Parameter(property = "javaPackages", required = true)
    private String javaPackages;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("begin to compile java to typescript!");
        List<String> packageList = Arrays.asList(javaPackages.split(";"));
        String targetDirectory = project.getBuild().getDirectory() + "/typescript";
        getLog().info("targetDirectory: " + targetDirectory);
        File sourceDirectory = new File(project.getBuild().getSourceDirectory());
        List<File> codeFiles = new ArrayList<>();
        CodeFileUtils.collectFiles(sourceDirectory, codeFiles);
        List<JavaFile> javaFiles = new ArrayList<>(codeFiles.size());
        codeFiles.forEach(c -> javaFiles.add(CodeFileUtils.buildJavaFileByCodeFile(c)));
        final List<JavaFile> targetJavaFile = javaFiles.stream().filter(j -> packageList.contains(j.getPackageName())).collect(Collectors.toList());
        for (JavaFile targetFile : targetJavaFile) {
            final List<String> codeLines = CodeFileUtils.readFileLines(targetFile.getCodeFile());
            if (codeLines.isEmpty()) {
                this.getLog().error("targetFile:[" + targetFile.getCodeFile().getAbsolutePath() + "] is empty code file");
                return;
            }
            final List<String> typescriptCode = convert2typescript(codeLines, targetFile.getClassName());
            try {
                final String targetFileName =
                        targetDirectory + "/" + targetFile.getPackageName().replace(".", "/") + "/" + targetFile.getClassName() + ".ts";
                FileUtils.writeStringToFile(new File(targetFileName), String.join("\n", typescriptCode), StandardCharsets.UTF_8);
            } catch (IOException e) {
                this.getLog().error(e);
            }
        }
        getLog().info("end to compile java to typescript!,plz watch it to ：" + targetDirectory);
    }

    private List<String> convert2typescript(List<String> codeLines, String className) {
        List<String> typescriptLines = new ArrayList<>(codeLines.size());
        //去掉包名和import和java注解的代码正文
        final List<String> textLines = codeLines.stream().filter(c -> !c.startsWith("package") && !c.startsWith("import") && !c.startsWith("@"))
                .collect(Collectors.toList());
        int spaceCount = 0;
        for (String textLine : textLines) {
            //类声名
            if (textLine.startsWith("public class")) {
                final String classDeclaration = textLine.replace("public", "");
                typescriptLines.add(classDeclaration.replace("implements Serializable ", ""));
            }
            //注释全部照搬
            if (textLine.matches("^\\s*(/\\*|\\*|\\*/).*")) {
                if (spaceCount == 0) {
                    spaceCount = textLine.replaceAll("^\\s+", "").length() - textLine.replaceAll("^\\s+", "").replaceAll("\\S.*", "").length() + 1;
                }
                typescriptLines.add(textLine);
            }
            //暂时不处理内部类的情况，所以剩下的就是字段属性的声明，我们约定规范的java pojo类都应该是private XX xx的，如果不符合就报错
            if (textLine.matches("^\\s*private.*") && !textLine.contains("serialVersionUID")) {
                final Pair<String, String> typeAndVariableName = JavaTypeUtils.getTypeAndVariableName(textLine);
                if (typeAndVariableName == null) {
                    getLog().warn("className:[" + className + "]，code :[" + textLine + "] temporarily unable to handle,plz contact the author!");
                    continue;
                }
                String propertyType = typeAndVariableName.getLeft();
                String typescriptType;
                //根据里氏替换原则，这里集合约定都用List来声明，什么ArrayList、LinkList来声明的都是不规范的，既然不规范就自己转换哈
                if (propertyType.startsWith("List")) {
                    final String collectionRealType = JavaTypeUtils.getCollectionRealType(propertyType);
                    typescriptType = JavaTypeUtils.javaTypeConvertTypescriptType(collectionRealType) + "[]";
                } else if (propertyType.startsWith("Map") || propertyType.startsWith("HashMap")) {
                    final Pair<String, String> mapType = JavaTypeUtils.getMapType(propertyType);
                    if (mapType == null) {
                        typescriptType = "Map";
                    } else {
                        typescriptType = "Map<" + JavaTypeUtils.javaTypeConvertTypescriptType(mapType.getLeft()) + ","
                                + JavaTypeUtils.javaTypeConvertTypescriptType(mapType.getRight()) + ">";
                    }
                } else {
                    typescriptType = JavaTypeUtils.javaTypeConvertTypescriptType(propertyType);
                }
                String typescriptCode = typeAndVariableName.getRight().replace(";", "") + ":" + typescriptType;
                typescriptLines.add(new String(new char[spaceCount]).replace("\0", " ") + typescriptCode);
            }
            if (textLine.contains("}") && !typescriptLines.contains("}")) {
                typescriptLines.add(textLine.trim());
            }
        }
        typescriptLines.add("export default " + className);
        return typescriptLines;
    }

}

package com.liudaolunhuibl.plugin.mojo;

import com.liudaolunhuibl.plugin.pojo.JavaFile;
import com.liudaolunhuibl.plugin.helper.CodeFileHelper;
import com.liudaolunhuibl.plugin.helper.JavaCodeHelper;
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
import java.util.List;
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

    /**
     * 最后生成路径
     */
    private static final String FINAL_TARGET_DIR = "/typescript";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("begin to compile java to typescript!");
        //最后生成路径
        String targetDirectory = project.getBuild().getDirectory() + FINAL_TARGET_DIR;
        getLog().info("targetDirectory: " + targetDirectory);
        //找到配置的包下的java文件
        List<JavaFile> targetJavaFile = findJavaFilesFromTarget();
        if (targetJavaFile.isEmpty()) {
            this.getLog().error("config package:[" + javaPackages + "] is all has no code,plz check it");
            return;
        }
        //根据java文件生成typescript文件
        generateTypescriptFile(targetDirectory, targetJavaFile);
        getLog().info("end to compile java to typescript!,plz watch it to ：" + targetDirectory);

    }

    private void generateTypescriptFile(String targetDirectory, List<JavaFile> targetJavaFiles) {
        for (JavaFile targetFile : targetJavaFiles) {
            final List<String> typescriptCode = convert2typescript(targetFile);
            try {
                final String targetFileName =
                        targetDirectory + "/" + targetFile.getPackageName().replace(".", "/") + "/" + targetFile.getClassName() + ".ts";
                FileUtils.writeStringToFile(new File(targetFileName), String.join("\n", typescriptCode), StandardCharsets.UTF_8);
            } catch (IOException e) {
                this.getLog().error(e);
            }
        }
    }

    private List<JavaFile> findJavaFilesFromTarget() {
        List<String> packageList = Arrays.asList(javaPackages.split(";"));
        List<File> codeFiles = CodeFileHelper.getTargetPackageFile(project.getBuild().getSourceDirectory(), packageList);
        if (codeFiles.isEmpty()) {
            this.getLog().error("config package:[" + javaPackages + "] is all empty,plz check it");
            return new ArrayList<>();
        }
        List<JavaFile> targetJavaFile = new ArrayList<>(codeFiles.size());
        for (File c : codeFiles) {
            final JavaFile javaFile = CodeFileHelper.buildJavaFileByCodeFile(c);
            if (javaFile == null) {
                this.getLog().error("targetFile:[" + c.getAbsolutePath() + "] is empty code file");
                continue;
            }
            targetJavaFile.add(javaFile);
        }
        return targetJavaFile;
    }

    private List<String> convert2typescript(JavaFile javaFile) {
        List<String> typescriptLines = new ArrayList<>(javaFile.getCodeLines().size());
        //去掉包名和import和java注解的代码正文
        final List<String> textLines = javaFile.getCodeLines().stream()
                .filter(c -> !c.startsWith("package") && !c.startsWith("import") && !c.startsWith("@")).collect(Collectors.toList());
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
            //字段属性的声明，我们约定规范的java pojo类都应该是private XX xx的，如果不符合就报错
            if (textLine.matches("^\\s*private.*") && !textLine.contains("serialVersionUID")) {
                //如果属性给了默认值那么就只要等号前面的属性描述,注意这里最起码你属性和等号要有一个空格吧，没有的话代码就是没有格式化好的，也不处理
                if (textLine.contains("=")) {
                    textLine = textLine.split(" =")[0] + ";";
                }
                final Pair<String, String> typeAndVariableName = JavaCodeHelper.getTypeAndVariableName(textLine);
                if (typeAndVariableName != null) {
                    typescriptLines.add(generateTypescriptVariableCode(spaceCount, typeAndVariableName));
                } else {
                    getLog().warn("className:[" + javaFile.getClassName() + "]，code :[" + textLine
                            + "] temporarily unable to handle,plz contact the author!");
                }
            }
            //hack 暂不处理内部类，逻辑比较复杂，并且内部类在和前端交互的场景中是不太规范的写法，也就是说你提供给前端的接口的参数或者返回值里最好不要有内部类
            if (textLine.matches("\\s*public\\s+static\\s+class\\s+.*")) {
                //这里如果开始读取内部类就立即结束了，下面的代码不会在转换了，如果需要的话自己手动处理吧，有人提issue就优化，没有就不管了
                typescriptLines.add("}");
                break;
            }
            //表示一个类文件结束
            if (textLine.contains("}") && !typescriptLines.contains("}")) {
                typescriptLines.add(textLine.trim());
            }
        }
        typescriptLines.add("export default " + javaFile.getClassName() + ";");
        return typescriptLines;
    }

    private String generateTypescriptVariableCode(int spaceCount, Pair<String, String> typeAndVariableName) {
        String propertyType = typeAndVariableName.getLeft();
        String typescriptType;
        //根据里氏替换原则，这里集合约定都用List来声明，什么ArrayList、LinkList来声明的都是不规范的，既然不规范就自己转换哈
        if (propertyType.startsWith("List")) {
            final String collectionRealType = JavaCodeHelper.getCollectionRealType(propertyType);
            typescriptType = JavaCodeHelper.javaTypeConvertTypescriptType(collectionRealType) + "[]";
        } else if (propertyType.startsWith("Map") || propertyType.startsWith("HashMap")) {
            final Pair<String, String> mapType = JavaCodeHelper.getMapType(propertyType);
            if (mapType == null) {
                typescriptType = "Map";
            } else {
                typescriptType = "Map<" + JavaCodeHelper.javaTypeConvertTypescriptType(mapType.getLeft()) + ","
                        + JavaCodeHelper.javaTypeConvertTypescriptType(mapType.getRight()) + ">";
            }
        } else {
            typescriptType = JavaCodeHelper.javaTypeConvertTypescriptType(propertyType);
        }
        String typescriptCode = typeAndVariableName.getRight().replace(";", "") + ":" + typescriptType;
        return new String(new char[spaceCount]).replace("\0", " ") + typescriptCode;
    }

}

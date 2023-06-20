package com.liudaolunhuibl.plugin.mojo;

import com.liudaolunhuibl.plugin.pojo.JavaFile;
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

    /**
     * map内部类型正则
     */
    private static final Pattern MAP_PATTERN = Pattern.compile("<(.+?),(.+?)>");

    private static final Pattern MAP_TYPE_PATTERN = Pattern.compile("(private)\\s+(Map<[^>]+>)\\s+(\\w+);");

    private static final Pattern NORMAL_TYPE_PATTERN = Pattern.compile("(private)\\s+(\\w+)\\s+(\\w+);");

    private static final Pattern LIST_TYPE_PATTERN = Pattern.compile("(private)\\s+(List<[^>]+>)\\s+(\\w+);");

    private static final String COMMON_SRC_PATH = "/src/main/java/";

    private static final Map<String, String> TYPE_MAP = new HashMap<>();

    static {
        TYPE_MAP.put("byte", "number");
        TYPE_MAP.put("short", "number");
        TYPE_MAP.put("int", "number");
        TYPE_MAP.put("long", "number");
        TYPE_MAP.put("float", "number");
        TYPE_MAP.put("double", "number");
        TYPE_MAP.put("char", "string");
        TYPE_MAP.put("boolean", "boolean");
        TYPE_MAP.put("String", "string");
        TYPE_MAP.put("Byte", "number");
        TYPE_MAP.put("Short", "number");
        TYPE_MAP.put("Integer", "number");
        TYPE_MAP.put("Long", "number");
        TYPE_MAP.put("Float", "number");
        TYPE_MAP.put("Double", "number");
        TYPE_MAP.put("Boolean", "boolean");
        TYPE_MAP.put("Object", "object");
        TYPE_MAP.put("Date", "Date");
        TYPE_MAP.put("BigDecimal", "number");

    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("begin to compile java to typescript!");
        List<String> packageList = Arrays.asList(javaPackages.split(";"));
        String targetDirectory = project.getBuild().getDirectory() + "/typescript";
        getLog().info("targetDirectory: " + targetDirectory);
        File sourceDirectory = new File(project.getBuild().getSourceDirectory());
        List<File> codeFiles = new ArrayList<>();
        collectFiles(sourceDirectory, codeFiles);
        List<JavaFile> javaFiles = new ArrayList<>(codeFiles.size());
        codeFiles.forEach(c -> {
            String absolutePath = c.getAbsolutePath();
            String basePath = absolutePath.substring(absolutePath.lastIndexOf(COMMON_SRC_PATH) + COMMON_SRC_PATH.length());
            final String[] pathArr = basePath.split("/");
            String className = pathArr[pathArr.length - 1];
            String packageName = basePath.replace("/" + className, "").replace("/", ".");
            javaFiles.add(JavaFile.builder().className(className.replace(".java", "")).packageName(packageName).codeFile(c).build());
        });
        final List<JavaFile> targetJavaFile = javaFiles.stream().filter(j -> packageList.contains(j.getPackageName())).collect(Collectors.toList());
        for (JavaFile targetFile : targetJavaFile) {
            final List<String> codeLines = readFileLines(targetFile.getCodeFile());
            if (codeLines == null) {
                this.getLog().error("targetFile+" + targetFile.getCodeFile().getAbsolutePath() + "+ is empty code file");
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
                final Pair<String, String> typeAndVariableName = getTypeAndVariableName(textLine);
                if (typeAndVariableName == null) {
                    getLog().warn("className:[" + className + "]，code :[" + textLine + "] temporarily unable to handle,plz contact the author!");
                    continue;
                }
                String propertyType = typeAndVariableName.getLeft();
                String typescriptType;
                //根据里氏替换原则，这里集合约定都用List来声明，什么ArrayList、LinkList来声明的都是不规范的，既然不规范就自己转换哈
                if (propertyType.startsWith("List")) {
                    final String collectionRealType = getCollectionRealType(propertyType);
                    typescriptType = javaTypeConvertTypescriptType(collectionRealType) + "[]";
                } else if (propertyType.startsWith("Map") || propertyType.startsWith("HashMap")) {
                    final Pair<String, String> mapType = getMapType(propertyType);
                    if (mapType == null) {
                        typescriptType = "Map";
                    } else {
                        typescriptType =
                                "Map<" + javaTypeConvertTypescriptType(mapType.getLeft()) + "," + javaTypeConvertTypescriptType(mapType.getRight())
                                        + ">";
                    }
                } else {
                    typescriptType = javaTypeConvertTypescriptType(propertyType);
                }
                String typescriptCode = typeAndVariableName.getRight().replace(";", "") + ":" + typescriptType;
                typescriptLines.add(new String(new char[spaceCount]).replace("\0", " ") + typescriptCode);
            }
            if (textLine.contains("}") && !typescriptLines.contains("}")) {
                typescriptLines.add(textLine.trim());
            }
        }
        typescriptLines.add("export { " + className + " };");
        return typescriptLines;
    }

    private String javaTypeConvertTypescriptType(String propertyType) {
        return TYPE_MAP.get(propertyType) == null ? "any" : TYPE_MAP.get(propertyType);
    }

    private Pair<String, String> getMapType(String mapCode) {
        Matcher matcher = MAP_PATTERN.matcher(mapCode);
        if (matcher.find()) {
            return Pair.of(matcher.group(1).trim(), matcher.group(2).trim());
        }
        return null;
    }

    private String getCollectionRealType(String collectionType) {
        return collectionType.replace("List<", "").replace(">", "");
    }

    private Pair<String, String> getTypeAndVariableName(String codeLine) {
        Pattern pattern;
        if (codeLine.contains("List<")) {
            pattern = LIST_TYPE_PATTERN;
        } else if (codeLine.contains("Map<")) {
            pattern = MAP_TYPE_PATTERN;
        } else {
            pattern = NORMAL_TYPE_PATTERN;
        }
        Matcher matcher = pattern.matcher(codeLine);
        if (matcher.find()) {
            return Pair.of(matcher.group(2), matcher.group(3));
        }
        return null;

    }

    private List<String> readFileLines(File file) {
        List<String> lines = null;
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            this.getLog().error(e);
        }
        if (lines == null) {
            return null;
        }
        List<String> nonEmptyLines = new ArrayList<>();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                nonEmptyLines.add(line);
            }
        }
        return nonEmptyLines;
    }

    private void collectFiles(File directory, List<File> files) {
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                if (file.isDirectory()) {
                    collectFiles(file, files);
                } else {
                    files.add(file);
                }
            }
        }
    }

}

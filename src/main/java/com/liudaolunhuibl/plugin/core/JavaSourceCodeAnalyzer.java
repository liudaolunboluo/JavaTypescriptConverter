package com.liudaolunhuibl.plugin.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.liudaolunhuibl.plugin.context.LogContext;
import com.liudaolunhuibl.plugin.pojo.JavaFieldInfo;
import com.liudaolunhuibl.plugin.pojo.JavaFile;
import com.liudaolunhuibl.plugin.visitor.JavaFieldVisitor;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: JavaSourceCodeAnalyzer
 * @Description: 解析java源码的ast抽象语法树
 * @date 2023/8/23
 */
@UtilityClass
public class JavaSourceCodeAnalyzer {

    public List<JavaFile> generateJavaFileFromFile(File sourceCodeFile) {
        try {
            JavaParser javaParser = new JavaParser();
            ParseResult<CompilationUnit> compilationUnitParseResult = javaParser.parse(sourceCodeFile);
            JavaFieldVisitor fieldVisitor = new JavaFieldVisitor();
            if (!compilationUnitParseResult.getResult().isPresent()) {
                return null;
            }
            compilationUnitParseResult.getResult().get().accept(fieldVisitor, null);
            String packageName = compilationUnitParseResult.getResult().get().getPackageDeclaration().map(PackageDeclaration::getNameAsString)
                    .orElse("");
            final List<JavaFieldInfo> fieldInfos = fieldVisitor.getFieldInfos();
            Map<String, List<JavaFieldInfo>> groupFields = fieldInfos.stream().collect(Collectors.groupingBy(JavaFieldInfo::getClassName));
            //本源码java文件的java类名称
            String topClassName = compilationUnitParseResult.getResult().get().findFirst(ClassOrInterfaceDeclaration.class)
                    .map(ClassOrInterfaceDeclaration::getNameAsString).orElse("");
            List<JavaFile> result = new ArrayList<>();
            groupFields.forEach((className, javaFieldInfo) -> {
                //如果是顶部类的话就看一下使用到了哪些内部类，然后生成import
                if (className.equals(topClassName)) {
                    List<JavaFieldInfo> matchedFieldInfos = javaFieldInfo.stream()
                            .filter(info -> fieldVisitor.getInnerClassName().contains(info.getFieldType())).collect(Collectors.toList());
                    result.add(JavaFile.builder().className(className).packageName(packageName).absolutePath(sourceCodeFile.getAbsolutePath())
                            .fieldInfos(javaFieldInfo).classAuthor(fieldVisitor.getClassAuthor()).classCreateDate(fieldVisitor.getClassCreateDate())
                            .innerClass(matchedFieldInfos.stream().map(JavaFieldInfo::getFieldType).collect(Collectors.toList())).build());
                } else {
                    result.add(JavaFile.builder().className(className).packageName(packageName).absolutePath(sourceCodeFile.getAbsolutePath())
                            .fieldInfos(javaFieldInfo).classAuthor(fieldVisitor.getClassAuthor()).classCreateDate(fieldVisitor.getClassCreateDate())
                            .build());
                }
            });
            return result;
        } catch (Exception e) {
            LogContext.getLog().error("分析" + sourceCodeFile.getName() + "文件失败!=", e);
            return new ArrayList<>();
        }
    }
}

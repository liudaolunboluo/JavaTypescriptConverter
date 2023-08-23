package com.liudaolunhuibl.plugin.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.liudaolunhuibl.plugin.context.LogContext;
import com.liudaolunhuibl.plugin.pojo.JavaFile;
import com.liudaolunhuibl.plugin.visitor.JavaFieldVisitor;
import lombok.experimental.UtilityClass;

import java.io.File;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: AstHelper
 * @Description: 解析java源码的ast抽象语法树
 * @date 2023/8/23
 */
@UtilityClass
public class JavaSourceCodeAnalyzer {

    public JavaFile generateJavaFileFromFile(File sourceCodeFile) {
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
            // 获取类名
            String className = compilationUnitParseResult.getResult().get().findFirst(ClassOrInterfaceDeclaration.class)
                    .map(ClassOrInterfaceDeclaration::getNameAsString).orElse("");
            return JavaFile.builder().className(className).packageName(packageName).absolutePath(sourceCodeFile.getAbsolutePath())
                    .fieldInfos(fieldVisitor.getFieldInfos()).classAuthor(fieldVisitor.getClassAuthor())
                    .classCreateDate(fieldVisitor.getClassCreateDate()).build();
        } catch (Exception e) {
            LogContext.getLog().error("分析" + sourceCodeFile.getName() + "文件失败!=", e);
            return null;
        }
    }
}

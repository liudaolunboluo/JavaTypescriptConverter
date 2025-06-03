package com.liudaolunhuibl.plugin.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.javadoc.Javadoc;
import com.google.inject.internal.util.Lists;
import com.liudaolunhuibl.plugin.pojo.JavaFieldInfo;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: JavaFieldVisitor
 * @Description: java字段解析ast
 * @date 2023/8/23
 */
@Getter
public class JavaFieldVisitor extends VoidVisitorAdapter<Void> implements Serializable {

    private static final String DEFAULT_SERIAL_VERSION_UID = "serialVersionUID";

    private final List<JavaFieldInfo> fieldInfos = Lists.newArrayList();

    private String classAuthor;

    private String classCreateDate;

    private final Set<String> innerClassName = new HashSet<>();

    @Override
    public void visit(FieldDeclaration fieldDeclaration, Void arg) {
        super.visit(fieldDeclaration, arg);
        final Optional<VariableDeclarator> first = fieldDeclaration.getVariables().getFirst();
        if (first.isEmpty()) {
            return;
        }
        String fieldName = first.get().getNameAsString();
        if (DEFAULT_SERIAL_VERSION_UID.equals(fieldName)) {
            return;
        }

        final JavaFieldInfo javaFieldInfo = generateJavaFieldInfo(fieldDeclaration, fieldName);
        if (javaFieldInfo != null) {
            fieldInfos.add(javaFieldInfo);
        }
    }

    @Override
    public void visit(RecordDeclaration recordDeclaration, Void arg) {
        super.visit(recordDeclaration, arg);
        generateJavaFieldInfo(recordDeclaration);

    }

    private void generateJavaFieldInfo(RecordDeclaration fieldDeclaration) {
        Node parentNode = fieldDeclaration.getParentNode().orElse(null);
        if (parentNode == null) {
            return;
        }
        final Optional<TypeDeclaration<?>> first = ((CompilationUnit) parentNode).getTypes().getFirst();
        if (first.isEmpty()) {
            return;
        }
        String className = first.get().getNameAsString();
        final NodeList<Parameter> parameters = fieldDeclaration.getParameters();
        parameters.forEach(parameter -> {
            final JavaFieldInfo javaFieldInfo = buildFieldInfo(className, parameter.getType(),
                    parameter.getComment().orElse(new JavadocComment(StringUtils.EMPTY)), parameter.getNameAsString());
            fieldInfos.add(javaFieldInfo);
        });
    }

    private JavaFieldInfo buildFieldInfo(String className, Type fieldType, @NonNull Comment comment, String fieldName) {
        // 检查字段类型是否为泛型类型 genericType
        List<String> genericType = Lists.newArrayList();
        if (fieldType instanceof ClassOrInterfaceType classOrInterfaceType) {
            // 获取字段类型的泛型参数列表
            Optional<NodeList<Type>> typeArguments = classOrInterfaceType.getTypeArguments();
            //泛型处理，List一个，Map两个，其他自定义类的泛型不处理
            if (typeArguments.isPresent()) {
                for (Type typeArgument : typeArguments.get()) {
                    String typeArgumentString = typeArgument.asString();
                    genericType.add(typeArgumentString);
                }
            }
        }

        final JavaFieldInfo javaFieldInfo = JavaFieldInfo.builder().fieldName(fieldName).fieldType(fieldType.asString()).className(className)
                .comment(comment.getContent().replace("*", "").replaceAll("[\r\n]", "").trim()).isCollection(false).isMap(false).build();
        if (genericType.size() == 1) {
            javaFieldInfo.setIsCollection(true);
            javaFieldInfo.setCollGenericType(genericType.getFirst());
        }
        if (genericType.size() == 2) {
            javaFieldInfo.setIsMap(true);
            javaFieldInfo.setMapGenericType(Pair.of(genericType.get(0), genericType.get(1)));
        }
        return javaFieldInfo;
    }

    private JavaFieldInfo generateJavaFieldInfo(FieldDeclaration fieldDeclaration, String fieldName) {
        Node parentNode = fieldDeclaration.getParentNode().orElse(null);
        if (parentNode == null) {
            return null;
        }
        String className = ((ClassOrInterfaceDeclaration) parentNode).getNameAsString();
        return buildFieldInfo(className, fieldDeclaration.getCommonType(),
                fieldDeclaration.getComment().orElse(new JavadocComment(StringUtils.EMPTY)), fieldName);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classDeclaration, Void arg) {
        super.visit(classDeclaration, arg);
        Optional<Javadoc> javadocOptional = classDeclaration.getJavadoc();
        if (javadocOptional.isPresent()) {
            Javadoc javadoc = javadocOptional.get();
            javadoc.getBlockTags().stream().filter(b -> "author".equals(b.getTagName())).findFirst()
                    .ifPresent(b -> classAuthor = b.getContent().toText());
            javadoc.getBlockTags().stream().filter(b -> "date".equals(b.getTagName())).findFirst()
                    .ifPresent(b -> classCreateDate = b.getContent().toText());
        }
    }

}

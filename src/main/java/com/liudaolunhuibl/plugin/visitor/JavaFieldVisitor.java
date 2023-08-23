package com.liudaolunhuibl.plugin.visitor;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.javadoc.Javadoc;
import com.google.inject.internal.util.Lists;
import com.liudaolunhuibl.plugin.pojo.JavaFieldInfo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

/**
 * @author yunfanzhang@kuainiugroup.com
 * @version 1.0
 * @ClassName: JavaFieldVisitor
 * @Description: java字段解析ast
 * @date 2023/8/23
 */
public class JavaFieldVisitor extends VoidVisitorAdapter<Void> {

    private static final String serialVersionUID = "serialVersionUID";

    @Getter
    private final List<JavaFieldInfo> fieldInfos = Lists.newArrayList();

    @Getter
    private String classAuthor;

    @Getter
    private String classCreateDate;

    @Override
    public void visit(FieldDeclaration fieldDeclaration, Void arg) {
        super.visit(fieldDeclaration, arg);
        String fieldName = fieldDeclaration.getVariables().get(0).getNameAsString();
        Type fieldType = fieldDeclaration.getCommonType();
        // 检查字段类型是否为泛型类型 genericType
        List<String> genericType = Lists.newArrayList();
        if (fieldType instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType classOrInterfaceType = (ClassOrInterfaceType) fieldType;
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
        Optional<Comment> commentOptional = fieldDeclaration.getComment();
        String commentContent = StringUtils.EMPTY;
        if (commentOptional.isPresent()) {
            Comment comment = commentOptional.get();
            commentContent = comment.getContent();
        }
        if (!serialVersionUID.equals(fieldName)) {
            final JavaFieldInfo javaFieldInfo = JavaFieldInfo.builder().fieldName(fieldName).fieldType(fieldType.asString())
                    .comment(commentContent.replace("*", "").replaceAll("[\r\n]", "").trim()).isCollection(false).isMap(false).build();
            if (genericType.size() == 1) {
                javaFieldInfo.setIsCollection(true);
                javaFieldInfo.setCollGenericType(genericType.get(0));
            }
            if (genericType.size() == 2) {
                javaFieldInfo.setIsMap(true);
                javaFieldInfo.setMapGenericType(Pair.of(genericType.get(0), genericType.get(1)));
            }
            fieldInfos.add(javaFieldInfo);
        }
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration classDeclaration, Void arg) {
        super.visit(classDeclaration, arg);
        Optional<Javadoc> javadocOptional = classDeclaration.getJavadoc();
        if (javadocOptional.isPresent()) {
            Javadoc javadoc = javadocOptional.get();
            javadoc.getBlockTags().stream().filter(b -> b.getTagName().equals("author")).findFirst()
                    .ifPresent(b -> classAuthor = b.getContent().toText());
            javadoc.getBlockTags().stream().filter(b -> "date".equals(b.getTagName())).findFirst()
                    .ifPresent(b -> classCreateDate = b.getContent().toText());
        }
    }

}

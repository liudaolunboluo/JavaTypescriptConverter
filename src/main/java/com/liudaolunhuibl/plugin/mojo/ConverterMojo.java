package com.liudaolunhuibl.plugin.mojo;

import com.liudaolunhuibl.plugin.context.LogContext;
import com.liudaolunhuibl.plugin.core.JavaPojoToTypeScriptConverter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Arrays;
import java.util.List;

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
        LogContext.saveLog(getLog());
        LogContext.getLog().info("begin to convert java to typescript!");
        List<String> packageList = Arrays.asList(javaPackages.split(";"));
        String targetDirectory = project.getBuild().getDirectory() + FINAL_TARGET_DIR;
        final JavaPojoToTypeScriptConverter converter = JavaPojoToTypeScriptConverter.builder().targetPackageList(packageList)
                .sourceDirectory(project.getBuild().getSourceDirectory()).targetDirectory(targetDirectory).build();
        converter.convert();
        LogContext.clear();
    }

}
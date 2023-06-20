# JavaTypescriptConverter
java pojo类翻译成typescript代码

使用：

本路径下执行:`mvn clean install`

然后在需要使用此插件的项目的pom文件里配置：

```xml
            <plugin>
                <groupId>com.liudaolunhuibl</groupId>
                <artifactId>java-typescrpt-converter-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>TypescriptConverter</goal>
                        </goals>
                        <phase>package</phase>
                    </execution>
                </executions>
                <configuration>
                    <javaPackages>需要翻译的java文件的包路径，多个用英文分号隔开，例如:com.a.b.dto;com.d.r.bo</javaPackages>
                </configuration>
            </plugin>
```

然后可以在idea的右侧的Maven菜单里指定项目的Plugins里找到该插件，然后双击执行，也可以在命令行中执行：

````shell
mvn com.liudaolunhuibl:java-typescrpt-converter-maven-plugin:1.0-SNAPSHOT:TypescriptConverter
````

执行完毕之后在项目的target目录下会有一个typescript目录，里面就是生成的typescript代码。

注意：pojo类应该遵循规范，例如都用privat修饰属性、List和Map都是接口声明而不是HashMap或者ArrayList声明，目前暂不支持内部类和嵌套对象，如果是嵌套对象会用`any`代替，后续可能会支持。



版本规划todo：

1、支持controller文件生成ts的接口调用文件；

2、支持嵌套对象；

3、支持java代码自定义要转换的ts类型；

4、支持java代码自定义ts文件名；

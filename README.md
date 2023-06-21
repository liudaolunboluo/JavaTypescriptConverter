# JavaTypescriptConverter
java pojo类翻译成typescript代码

使用：

clone本项目，在源码路径下执行:`mvn clean install`

（主要是原因本项目还没有上传到maven中央仓库，所以大家自己手动凑合着整吧，反正都是本地行为）

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

在控制台中可以看到如下打印:

```shell
[INFO] begin to compile java to typescript!
```

就是开始翻译了（这里用的编译的单词并不正确，因为这不是编译，编译是将高级语言代码转换为低级语言代码的过程，例如将 Java 代码编译为字节码。而将 Java 代码转换为 TypeScript 代码只是将一种高级语言代码转换为另一种高级语言代码的过程，这种转换通常被称为代码转换或代码翻译。所以这里用编译只是作者装一下逼而已）

执行完毕之后在项目的target目录下会有一个typescript目录，里面就是生成的typescript代码。

注意：

- pojo类应该遵循规范，例如都用privat修饰属性、List和Map都是接口声明而不是HashMap或者ArrayList声明。

- **目前暂不支持静态内部类**，有静态内部类的生成会乱，但也不是不能用，自己手动改一下ts文件吧，后续可能会支持。

- 如果是嵌套对象，那么会生成一样的类型，例如:`private Student a`转换出来就是：`a:Student`所以需要把这个类型也拷贝到前端项目里或者自己手动改成`any`

- 不支持有继承关系的属性自动映射到子类中，继承关系会原封不动的到生成的ts代码里，也就是说你的父类必须也在你的前端项目里，如果不想可以手动拷贝父类属性到子类中。



版本规划todo：

1、支持controller文件生成ts的接口调用文件；

2、支持嵌套对象；

3、支持java代码自定义要转换的ts类型；

4、支持java代码自定义ts文件名；

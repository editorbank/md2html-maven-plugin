# Maven plugin for MarkDown To HTML converting

Build and install:
```
mvn clean install
```

To see what he can do:
```
mvn help:describe -DgroupId=com.github.editorbank -DartifactId=md2html-maven-plugin
```

Testing:
```
mvn md2html:md2html -P test
```
The `target/test.html` file will be generated during testing.
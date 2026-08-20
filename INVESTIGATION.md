# Investigate XLS parsing failure in packaged Gradle JAR and define required Excel dependencies

Issue https://github.com/Kotlin/dataframe/issues/422

## Reproducing

1. Create a test project with:
  - simple code reading an Excel file to a dataframe and printing the dataframe
  - the dependencies mentioned in the issue:
      * Ktor 2.3.2
      * Gradle 7.4
      * dataframe-excel:0.10.1
  - two simple files: sample.xls and sample2.xlsx
  Kotlin version used: 1.8.22
  JDK: jbr-17

2. Run the project:
    - `./gradlew run --args="data/sample.xls"` - SUCCESSFUL
    - `./gradlew run --args="data/sample2.xlsx"` - SUCCESSFUL

3. Create a fat jar and run it:
    - `./gradlew buildFatJar`
    - `java -jar build/libs/xls-parsing-issue-all.jar data/sample2.xlsx` - SUCCESSFUL
    - `java -jar build/libs/xls-parsing-issue-all.jar data/sample.xls` - Exception (see below)

Result:

```
Exception in thread "main" java.io.IOException: Your InputStream was neither an OLE2 stream, nor an OOXML stream or you haven't provide the poi-ooxml*.jar in the classpath/modulepath - FileMagic: OLE2, having providers: [org.apache.poi.xssf.usermodel.XSSFWorkbookFactory@19e1023e]
        at org.apache.poi.ss.usermodel.WorkbookFactory.wp(WorkbookFactory.java:334)
        at org.apache.poi.ss.usermodel.WorkbookFactory.create(WorkbookFactory.java:318)
        at org.apache.poi.ss.usermodel.WorkbookFactory.create(WorkbookFactory.java:277)
        at org.apache.poi.ss.usermodel.WorkbookFactory.create(WorkbookFactory.java:255)
        at org.jetbrains.kotlinx.dataframe.io.XlsxKt.readExcel(xlsx.kt:88)
        at org.jetbrains.kotlinx.dataframe.io.XlsxKt.readExcel$default(xlsx.kt:81)
        at io.github.allexnik.MainKt.main(Main.kt:8)
```
    
This is the exception reported in the issue.
  
## Diagnostics

1. Check the `poi` and `poi-ooxml` jars in the project
    - `poi` contains the `HSSFWorkbookFactory.class` file:
`.gradle/caches/modules-2/files-2.1/org.apache.poi/poi/5.2.2/5513d31545085c33809c4b6553c2009fd19a6016/poi-5.2.2.jar!/org/apache/poi/hssf/usermodel/HSSFWorkbookFactory.class`
and the service descriptor for the corresponding WorkbookProvider:
`.gradle/caches/modules-2/files-2.1/org.apache.poi/poi/5.2.2/5513d31545085c33809c4b6553c2009fd19a6016/poi-5.2.2.jar!/META-INF/services/org.apache.poi.ss.usermodel.WorkbookProvider`
with the following content: `org.apache.poi.hssf.usermodel.HSSFWorkbookFactory`

    - `poi-ooxml` contains the `XSSFWorkbookFactory.class` file:
`.gradle/caches/modules-2/files-2.1/org.apache.poi/poi-ooxml/5.2.2/a201b5bdc92c0fae4bed4b8e5546388c4c2f9eb0/poi-ooxml-5.2.2.jar!/org/apache/poi/xssf/usermodel/XSSFWorkbookFactory.class`
and the service descriptor for the corresponding WorkbookProvider:
`.gradle/caches/modules-2/files-2.1/org.apache.poi/poi-ooxml/5.2.2/a201b5bdc92c0fae4bed4b8e5546388c4c2f9eb0/poi-ooxml-5.2.2.jar!/META-INF/services/org.apache.poi.ss.usermodel.WorkbookProvider`
with the following content: `org.apache.poi.xssf.usermodel.XSSFWorkbookFactory`

Therefore, both packages responsible for reading `xls` and `xlsx` formats have their own `WorkbookFactory` class and their own service descriptor.

2. Unpack the fat jar and check its internals
Both `WorkbookFactory` classes are there: 
  - `xls-parsing-issue-all/org/apache/poi/hssf/usermodel/HSSFWorkbookFactory.class`
  - `xls-parsing-issue-all/org/apache/poi/xssf/usermodel/XSSFWorkbookFactory.class`

As for the corresponding service descriptors, only one is left in the fat jar:
`xls-parsing-issue-all/META-INF/services/org.apache.poi.ss.usermodel.WorkbookProvider`
This is a descriptor with the following content: `org.apache.poi.xssf.usermodel.XSSFWorkbookFactory`,
which is for the module responsible for reading `xlsx`.
And the service descriptor for `poi`, with `org.apache.poi.hssf.usermodel.HSSFWorkbookFactory`, is missing.

3. Add the missing class name to the service descriptor, pack the jar back and run it

    - Add the line `org.apache.poi.hssf.usermodel.HSSFWorkbookFactory` to the file in the jar:
    `xls-parsing-issue-all/META-INF/services/org.apache.poi.ss.usermodel.WorkbookProvider`
    so the content of the file becomes:
    
    ```
    org.apache.poi.hssf.usermodel.HSSFWorkbookFactory
    org.apache.poi.xssf.usermodel.XSSFWorkbookFactory
    ```

    - Pack the jar: `jar cfm ../app-modified.jar META-INF/MANIFEST.MF .` 

    - Run it:
        - `java -jar build/libs/app-modified.jar data/sample.xls` - SUCCESSFUL
        - `java -jar build/libs/app-modified.jar data/sample2.xlsx` - SUCCESSFUL

Clearly, after this change the problem disappears.

## Solutions

1. Use Ktor 3.5.2+

The issue disappears if we create and run the jar using the `buildFatJar` gradle task from Ktor 3.5.2+.
Other dependencies need to be compatible with this Ktor version. 
In this project, the solution was verified with the following dependencies:

```
kotlin 2.4.0
ktor 3.5.2
dataframe-excel:1.0.0-rc01
Gradle 9.0
```

Why it works:
Ktor 3.5.2 release (August 4, 2026) reports the following bugfix: 
KTOR-8992 HoconConfigLoader is not loaded when ktor-server-config-yaml on the classpath.
The link to the ticket: https://youtrack.jetbrains.com/issue/KTOR-8992

Addressing this ticket included the following commit: 
https://github.com/ktorio/ktor-build-plugins/commit/11489c012aa0c6a47e0565684c22a0047af7b81d
which added `shadowJar.mergeServiceFiles()` to the task.

With this option, the `org.apache.poi.ss.usermodel.WorkbookProvider` service descriptor files 
from `poi` and `poi-ooxml` are merged correctly, which solves the problem.

2. Use the Shadow plugin with correct duplicates strategy and resource transformer

If you use the Shadow plugin (or the Ktor plugin with the version older than 3.5.2), you need to:
    - override the default `EXCLUDE` duplicates strategy with `INCLUDE` 
(so that the Gradle task does not attempt to prevent duplicates), and
    - set up a `ResourceTransformer` with `mergeServiceFiles()`.

```
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy

tasks.named<ShadowJar>("shadowJar") {
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    mergeServiceFiles()
}
```

See https://gradleup.com/shadow/configuration/merging/ for details.

This configuration also solves the problem.

3. Use distribution instead of a fat jar

If using fat jar is not necessary, you can also pack the project as distribution: `./gradlew installDist`.

Then it can be run without problems:
    - `build/install/xls-parsing-issue/bin/xls-parsing-issue data/sample.xls` - SUCCESS
    - `build/install/xls-parsing-issue/bin/xls-parsing-issue data/sample2.xlsx` - SUCCESS

To put it into an archive, use `./gradlew distZip`.
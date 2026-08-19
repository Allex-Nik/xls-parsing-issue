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
  

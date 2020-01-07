# About
Automatically generate the classes required when using [graphql-java-tools](https://github.com/graphql-java-kickstart/graphql-java-tools) in kotlin.

# How to use
Download from [releases](https://github.com/ryo-chin/graphql-generator/releases) and put under classpath it.

Add plugin settings to build.gradle.
```groovy
// build.gradle
plugins {
    id "com.github.ryo-chin.graphql-generator" version "0.0.1-SNAPSHOT"
}

graphql_generator {
    inputPath = 'src/main/resources/graphql/schema.graphql' 
    // # if you want to generate all graphql files under the directory 
    // inputPath = 'src/main/resources/graphql'
    outputPath = 'src/main/kotlin/graphql/autogen'
    // ID type (default = 'String')
    idType = 'Long'
}
```
Execute gradle task.
```bash
./gradlew generate
```

# Release
```bash
./gradlew generator:publishPlugins -Dgradle.publish.key=<key> -Dgradle.publish.secret=<secret>
```

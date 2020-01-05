# About
Automatically generate the classes required when using [graphql-java-tools](https://github.com/graphql-java-kickstart/graphql-java-tools) in kotlin.

# How to use
Download from [releases](https://github.com/ryo-chin/graphql-generator/releases) and put under classpath it.

Add plugin settings to build.gradle.
```groovy
// build.gradle
buildscript {
    dependencies {
        classpath files('graphql-generator-0.0.1-SNAPSHOT.jar') // TODO: fix if approved by gradle portal 
    }
}

apply plugin: 'com.github.ryo-chin.graphql-generator'

graphql_generator {
    inputPath = 'src/main/resources/graphql/schema.graphql' 
    // # if you want to generate all graphql files under the directory 
    // inputPath = 'src/main/resources/graphql'
    outputPath = 'src/main/kotlin/graphql/autogen'
    idType = 'Long'
}
```
Execute gradle task.
```bash
./gradlew generate
```

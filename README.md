# How to use
Add plugin settings to build.gradle.
```groovy
// build.gradle
buildscript {
    dependencies {
        classpath files('graphql-generator-0.0.1.jar') // TODO: fix if approved by gradle portal 
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
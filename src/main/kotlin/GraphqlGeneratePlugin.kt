import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.InputStreamReader

/**
 * @author hakiba
 */
class GraphqlGeneratePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            tasks.create("generate").doLast {
                val schemaPath = "src/test/resources/graphql/schema.graphql"
                val outputDirPath = "tmp/autogen"
                val idType = System.getProperty("idType")?.let { IDType.valueOf(it) } ?: IDType.String

                val generator = Generator(idType)

                generator.read(schemaPath)
                        .flatMap { generator.parse(InputStreamReader(it.inputStream()).buffered().readText()) }
                        .forEach { generator.generate(outputDirPath, it) }
            }
        }
    }
}
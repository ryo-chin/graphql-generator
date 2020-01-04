import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author hakiba
 */
class GraphqlGeneratePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            tasks.create("generate").doLast {
                val inputPath = "src/test/resources/graphql/schema.graphql"
                val outputPath = "tmp/autogen"
                main(arrayOf(inputPath, outputPath))
            }
        }
    }
}
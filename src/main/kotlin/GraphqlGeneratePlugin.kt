import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author hakiba
 */
class GraphqlGeneratePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("graphql_generator", GraphqlGenerateConfiguration::class.java)
        project.run {
            tasks.create("generate").doLast {
                val config = project.extensions.getByType(GraphqlGenerateConfiguration::class.java)
                System.setProperty("idType", config.idType)
                main(arrayOf(config.inputPath, config.outputPath))
            }
        }
    }
}

open class GraphqlGenerateConfiguration{
    lateinit var inputPath: String
    lateinit var outputPath: String
    var idType: String = "String"
}

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * @author hakiba
 */
class GraphqlGeneratePluginTest {
    @Test
    fun test() {
        val project = ProjectBuilder.builder().build()
        val graphqlGeneratePlugin = GraphqlGeneratePlugin()
        graphqlGeneratePlugin.apply(project)
        val configuration = project?.property("graphql_generator") as GraphqlGenerateConfiguration
        configuration.apply {
            inputPath = ""
            outputPath = ""
        }
        val task = project.tasks.findByName("generateGraphQLFiles")
        assertNotNull(task)
//        task?.actions?.get(0)?.execute(task)
    }
}

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.InputStreamReader

/**
 * @author hakiba
 */
class GeneratorTest {
    private val generator = Generator()

    @Test
    fun readSchemaFile() {
        val expected = Generator::class.java.classLoader.getResource("graphql/schema.graphql")
                ?.let { InputStreamReader(it.openStream()).buffered() }

        val actual = generator.read("src/test/resources/graphql/schema.graphql")
                ?.let { InputStreamReader(it.inputStream()).buffered() }

        assertEquals(expected?.readText(), actual?.readText())
    }

    @Test
    fun parseSchema() {
        val input = """
            type User implements Node {
                id: ID!
                username: String!
                email: String
                role: Role!
            }
        """.trimIndent()
        val actual = generator.parse(input).first()
        assertEquals("User", actual.name)
        val actualId = actual.fields.first { it.name == "id" }
        assertEquals("String", actualId.type)
        val actualUsername = actual.fields.first { it.name == "username" }
        assertEquals("String", actualUsername.type)
        val actualEmail = actual.fields.first { it.name == "email" }
        assertEquals("String?", actualEmail.type)
        val actualRole = actual.fields.first { it.name == "role" }
        assertEquals("Role", actualRole.type)
    }

    // done: optional type(?) parse
    // TODO: built-in scalar type (Long, Integer...)
    // TODO: List type
    // TODO: type interface
    // TODO: ID type setting (Long, Integer...)
}

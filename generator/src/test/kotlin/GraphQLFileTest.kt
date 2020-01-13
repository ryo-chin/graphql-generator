import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.stream.Collectors

/**
 * @author hakiba
 */
class GraphQLFileTest {
    private val reader = GraphQLFileReader()
    private val parser = GraphQLFileParser()

    @Test
    fun readSchemaFile() {
        val expected = GraphQLFileReader::class.java.classLoader.getResource("graphql/schema.graphql")
                ?.let { InputStreamReader(it.openStream()).buffered() }

        val actual = reader.read("src/test/resources/graphql/schema.graphql").first()

        Assertions.assertEquals(expected?.readText(), actual.readText())
    }

    @Test
    fun readSchemaFileFromDir() {
        val resource = GraphQLFileReader::class.java.classLoader.getResource("graphql")!!
        val expected = resource
                .let { File(it.toURI()).toPath() }
                .let { Files.list(it) }.collect(Collectors.toList())
                .map { it.toFile() }
                .groupBy({ it.name }, { InputStreamReader(it.inputStream()).buffered().readText() })

        val actual = reader.read("src/test/resources/graphql")

        actual.forEach {
            val exp = expected[it.name()]?.firstOrNull()
            Assertions.assertNotNull(exp, "${it.name()} is not exists")
            Assertions.assertEquals(exp, it.readText())
        }
    }


    @Test
    fun parseObjectTypeDocument() {
        val input = """
            type User implements Node {
                id: ID!
                nullableId: ID
                username: String!
                email: String
                role: Role!
                phoneNumbers: [String!]!
                posts: [Post!]
                nestedList: [[String!]!]!
                nestedNullableList: [[String]]
            }
        """.trimIndent()
        val actual = parser.parseObjectTypeDocument(input).first()

        Assertions.assertEquals("User", actual.name)
        val actualId = actual.fields.first { it.name == "id" }
        Assertions.assertEquals("ID", actualId.type)
        val actualNullableId = actual.fields.first { it.name == "nullableId" }
        Assertions.assertEquals("ID?", actualNullableId.type)
        val actualUsername = actual.fields.first { it.name == "username" }
        Assertions.assertEquals("String", actualUsername.type)
        val actualEmail = actual.fields.first { it.name == "email" }
        Assertions.assertEquals("String?", actualEmail.type)
        val actualRole = actual.fields.first { it.name == "role" }
        Assertions.assertEquals("Role", actualRole.type)
        val actualPhoneNumbers = actual.fields.first { it.name == "phoneNumbers" }
        Assertions.assertEquals("List<String>", actualPhoneNumbers.type)
        val actualNestedList = actual.fields.first { it.name == "nestedList" }
        Assertions.assertEquals("List<List<String>>", actualNestedList.type)
        val actualNestedNullableList = actual.fields.first { it.name == "nestedNullableList" }
        Assertions.assertEquals("List<List<String?>?>?", actualNestedNullableList.type)
    }

    @Test
    fun parseObjectTypeDocument_BuiltInScalarType() {
        val input = """
            type Scalars {
                int: Int!
                float: Float!
                boolean: Boolean!
            }
        """.trimIndent()

        val actual = parser.parseObjectTypeDocument(input).first()

        val actualInt = actual.fields.first { it.name == "int" }
        Assertions.assertEquals("Int", actualInt.type)
        val actualFloat = actual.fields.first { it.name == "float" }
        Assertions.assertEquals("Float", actualFloat.type)
        val actualBoolean = actual.fields.first { it.name == "boolean" }
        Assertions.assertEquals("Boolean", actualBoolean.type)
    }

    @Test
    fun convertBody() {
        val input = """
            type User implements Node {
                id: ID!
                username: String!
                email: String
                role: Role!
            }
        """.trimIndent()
        val expected = """
            data class User(
                val id: String,
                val username: String,
                val email: String?,
                val role: Role
            )
        """.trimIndent()
        val parsed = parser.parseObjectTypeDocument(input).first()

        val actual = parsed.convertBody()

        Assertions.assertEquals(expected, actual.trimIndent())
    }

    @Test
    fun convertBodyWithIDType() {
        val input = """
            type User implements Node {
                id: ID!
                nullableId: ID
                idList: [ID!]!
                nullableIdList: [ID]
            }
        """.trimIndent()

        val expected = """
            data class User(
                val id: Long,
                val nullableId: Long?,
                val idList: List<Long>,
                val nullableIdList: List<Long?>?
            )
        """.trimIndent()


        val parsed = parser.parseObjectTypeDocument(input).first()
        val actual = parsed.convertBody(IDType.Long)

        Assertions.assertEquals(expected, actual.trimIndent())
    }
}

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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

        assertEquals(expected?.readText(), actual.readText())
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
            assertNotNull(exp, "${it.name()} is not exists")
            assertEquals(exp, it.readText())
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

        assertEquals("User", actual.name)
        val actualId = actual.fields.first { it.name == "id" }
        assertEquals("ID", actualId.type)
        val actualNullableId = actual.fields.first { it.name == "nullableId" }
        assertEquals("ID?", actualNullableId.type)
        val actualUsername = actual.fields.first { it.name == "username" }
        assertEquals("String", actualUsername.type)
        val actualEmail = actual.fields.first { it.name == "email" }
        assertEquals("String?", actualEmail.type)
        val actualRole = actual.fields.first { it.name == "role" }
        assertEquals("Role", actualRole.type)
        val actualPhoneNumbers = actual.fields.first { it.name == "phoneNumbers" }
        assertEquals("List<String>", actualPhoneNumbers.type)
        val actualNestedList = actual.fields.first { it.name == "nestedList" }
        assertEquals("List<List<String>>", actualNestedList.type)
        val actualNestedNullableList = actual.fields.first { it.name == "nestedNullableList" }
        assertEquals("List<List<String?>?>?", actualNestedNullableList.type)
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
        assertEquals("Int", actualInt.type)
        val actualFloat = actual.fields.first { it.name == "float" }
        assertEquals("Float", actualFloat.type)
        val actualBoolean = actual.fields.first { it.name == "boolean" }
        assertEquals("Boolean", actualBoolean.type)
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

        assertEquals(expected, actual.trimIndent())
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

        assertEquals(expected, actual.trimIndent())
    }

    @Test
    fun kotlinClassFileName() {
        val expected = "StandardUserProfile"
        listOf(
                "StandardUserProfile.graphql",
                "standardUserProfile.graphql",
                "standard-user-profile.graphql",
                "standard_user_profile.graphql",
                "standard-user_profile.graphql"
        ).forEach {
            val file = File(it)
            val actual = GraphQLFile(file).kotlinClassFileName()
            assertEquals(expected, actual, it)
        }
    }
}

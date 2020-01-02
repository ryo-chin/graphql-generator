import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.InputStreamReader

/**
 * @author hakiba
 */
class GeneratorTest {
    private val generator = Generator()

    @Test // for test execute
    fun testMain() {
        System.setProperty("idType", "Long")
        main(arrayOf("src/test/resources/graphql/schema.graphql", "tmp/autogen"))
    }

    @Test
    fun readSchemaFile() {
        val expected = Generator::class.java.classLoader.getResource("graphql/schema.graphql")
                ?.let { InputStreamReader(it.openStream()).buffered() }

        val actual = generator.read("src/test/resources/graphql/schema.graphql")
                .let { InputStreamReader(it.inputStream()).buffered() }

        assertEquals(expected?.readText(), actual.readText())
    }

    // done: optional type(?) parse
    // done: List type
    // done: nullable ID
    @Test
    fun parseSchema() {
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

        val actual = generator.parse(input).first()

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

    // done: built-in scalar type
    @Test
    fun parseSchemaBuiltInScalarType() {
        val input = """
            type Scalars {
                int: Int!
                float: Float!
                boolean: Boolean!
            }
        """.trimIndent()

        val actual = generator.parse(input).first()

        val actualInt = actual.fields.first { it.name == "int" }
        assertEquals("Int", actualInt.type)
        val actualFloat = actual.fields.first { it.name == "float" }
        assertEquals("Float", actualFloat.type)
        val actualBoolean = actual.fields.first { it.name == "boolean" }
        assertEquals("Boolean", actualBoolean.type)
    }

    // TODO: enum
    // TODO: type interface

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
        val parsed = generator.parse(input).first()

        val actual = parsed.convertBody()

        assertEquals(expected, actual.trimIndent())
    }

    // done: ID type setting (Long, Integer...)
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


        val parsed = generator.parse(input).first()
        val actual = parsed.convertBody(IDType.Long)

        assertEquals(expected, actual.trimIndent())
    }


    @Test
    fun generateKotlinFile() {
        val input = """
            type User implements Node {
                id: ID!
                username: String!
                email: String
                role: Role!
            }
        """.trimIndent()
        val expected = """
            | data class User(
            |     val id: String,
            |     val username: String,
            |     val email: String?,
            |     val role: Role
            | )
            | 
        """.trimMargin("| ")

        val fileName = "User"
        val outputDirPath = "tmp/autogen"

        generator.generate(outputDirPath, generator.parse(input).first())

        val actualFile = File("${System.getProperty("user.dir")}/$outputDirPath/$fileName.kt")
        val actualBody = InputStreamReader(actualFile.inputStream()).buffered().readText()
        assertTrue(actualFile.exists())
        assertEquals(expected, actualBody)
    }
}

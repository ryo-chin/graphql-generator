
import io.kotlintest.data.suspend.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.util.stream.Collectors

/**
 * @author hakiba
 */
class GeneratorTest {
    private val generator = Generator()
    // done: optional type(?) parse
    // done: List type
    // done: nullable ID
    // done: built-in scalar type
    // done: ID type setting (Long, Integer...)
    // done: read multiple file
    // done: generate from multiple file
    // done: execute by gradle (or native image)
    // done: configuration by build.gradle
    // done: public plugin
    // done: add package
    // done: one kotlin file output
    // TODO?: not exists dir create
    // TODO: generate input
    // TODO: generate full query
    // TODO: generate resolver
    // TODO: generate enum
    // TODO: implements type's interface


    @Test // for test execute
    fun testMain() {
        System.setProperty("idType", "Long")
        main(arrayOf("src/test/resources/graphql/schema.graphql", "tmp/autogen"))
    }

    class IntegrationTests : StringSpec({
        "standard" {
            val inputDirPath = "src/test/resources/graphql/test"
            val outputDirPath = "src/main/kotlin/graphql/autogen"
            val idType = "String"
            val graphqlFiles = inputGraphqlFiles
            val expected = mapOf("User.kt" to """
                | package graphql.autogen
                | 
                | data class User(
                |     val id: String,
                |     val username: String,
                |     val email: String,
                |     val role: Role
                | )
                | 
            """.trimMargin("| "), "Chat.kt" to """
                | package graphql.autogen
                | 
                | data class Chat(
                |     val id: String,
                |     val users: List<User>,
                |     val messages: List<ChatMessage>
                | )
                | 
            """.trimMargin("| "), "ChatMessage.kt" to """
                | package graphql.autogen
                | 
                | data class ChatMessage(
                |     val id: String,
                |     val content: String,
                |     val time: Date,
                |     val user: User
                | )
                | 
            """.trimMargin("| "))

            integrationTest(inputDirPath, graphqlFiles, outputDirPath, expected, idType)
        }

        "not exist package" {
            val inputDirPath = "src/test/resources/graphql/test"
            val outputDirPath = "tmp/graqhql/autogen"
            val idType = "String"
            val graphqlFiles = inputGraphqlFiles
            val expected = mapOf("User.kt" to """
                | data class User(
                |     val id: String,
                |     val username: String,
                |     val email: String,
                |     val role: Role
                | )
                | 
            """.trimMargin("| "), "Chat.kt" to """
                | data class Chat(
                |     val id: String,
                |     val users: List<User>,
                |     val messages: List<ChatMessage>
                | )
                | 
            """.trimMargin("| "), "ChatMessage.kt" to """
                | data class ChatMessage(
                |     val id: String,
                |     val content: String,
                |     val time: Date,
                |     val user: User
                | )
                | 
            """.trimMargin("| "))

            integrationTest(inputDirPath, graphqlFiles, outputDirPath, expected, idType)
        }

        "one file output" {
            val inputDirPath = "src/test/resources/graphql/test"
            val outputDirPath = "src/main/kotlin/graphql/autogen"
            val idType = "String"
            val graphqlFiles = inputGraphqlFiles
            val expected = mapOf("User.kt" to """
                | package graphql.autogen
                | 
                | data class User(
                |     val id: String,
                |     val username: String,
                |     val email: String,
                |     val role: Role
                | )
                | 
            """.trimMargin("| "), "Chat.kt" to """
                | package graphql.autogen
                | 
                | data class Chat(
                |     val id: String,
                |     val users: List<User>,
                |     val messages: List<ChatMessage>
                | )
                | 
                | data class ChatMessage(
                |     val id: String,
                |     val content: String,
                |     val time: Date,
                |     val user: User
                | )
                | 
            """.trimMargin("| "))

            System.setProperty("outputMode", "OneToOne")
            integrationTest(inputDirPath, graphqlFiles, outputDirPath, expected, idType)
            System.setProperty("outputMode", "")
        }
    })

    @Test
    fun readSchemaFile() {
        val expected = Generator::class.java.classLoader.getResource("graphql/schema.graphql")
                ?.let { InputStreamReader(it.openStream()).buffered() }

        val actual = generator.read("src/test/resources/graphql/schema.graphql").first()
                .let { InputStreamReader(it.inputStream()).buffered() }

        assertEquals(expected?.readText(), actual.readText())
    }

    @Test
    fun readSchemaFileFromDir() {
        val resource = Generator::class.java.classLoader.getResource("graphql")!!
        val expected = resource
                .let { File(it.toURI()).toPath() }
                .let { Files.list(it) }.collect(Collectors.toList())
                .map { it.toFile() }
                .groupBy({ it.name }, { InputStreamReader(it.inputStream()).buffered().readText() })

        val actual = generator.read("src/test/resources/graphql")

        actual.forEach {
            val exp = expected[it.name]?.firstOrNull()
            assertNotNull(exp, "${it.name} is not exists")
            assertEquals(exp, InputStreamReader(it.inputStream()).buffered().readText())
        }
    }

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

    class ParsePackageSpec : StringSpec({
        val generator = Generator()
        "parse package name" {
            forall(
                    row("src/main/kotlin/graphql", "graphql"),
                    row("generator/src/main/kotlin/graphql", "graphql"),
                    row("generator/src/main/kotlin/graphql/autogen", "graphql.autogen"),
                    row("generator/src/main/kotlin/graphql/autogen/", "graphql.autogen"),
                    row("generator/src/main/kotlin", null),
                    row("src/test/kotlin/graphql", "graphql"),
                    row("generator/src/test/kotlin/graphql", "graphql"),
                    row("generator/src/test/kotlin/graphql/autogen", "graphql.autogen"),
                    row("generator/src/test/kotlin/graphql/autogen/", "graphql.autogen"),
                    row("generator/src/test/kotlin", null),
                    row("generator/tmp/autogen/", null)
            )
            { input, expected ->
                generator.parsePackageName(input) shouldBe expected
            }
        }
    })
}

private fun integrationTest(inputDirPath: String, inputs: Map<String, String>, outputDirPath: String, expected: Map<String, String>, idType: String) {
    val rootDir = System.getProperty("user.dir")
    val sourceDir = File("$rootDir/$inputDirPath")
    Files.createDirectories(sourceDir.toPath())

    inputs.forEach {
        val writer = OutputStreamWriter(File("$rootDir/$inputDirPath/${it.key}").outputStream()).buffered()
        writer.write(it.value)
        writer.flush()
        writer.close()
    }

    System.setProperty("idType", idType)
    main(arrayOf(inputDirPath, outputDirPath))

    val outputDir = File("$rootDir/$outputDirPath")
    val actual = Files.list(outputDir.toPath()).collect(Collectors.toList())
            .map { it.toFile() }
            .groupBy({ it.name }, { InputStreamReader(it.inputStream()).buffered().readText() })
    expected.forEach {
        val body = actual[it.key]?.firstOrNull()
        body shouldNotBe null
        body shouldBe it.value
    }

    Files.list(sourceDir.toPath()).forEach {
        Files.deleteIfExists(it)
    }
    Files.deleteIfExists(sourceDir.toPath())
    Files.list(outputDir.toPath()).forEach {
        Files.deleteIfExists(it)
    }
    Files.deleteIfExists(outputDir.toPath())
}

val inputGraphqlFiles = mapOf("user.graphql" to """
                type User implements Node {
                    id: ID!
                    username: String!
                    email: String!
                    role: Role!
                }
            """.trimIndent(), "chat.graphql" to """
                type Chat implements Node {
                    id: ID!
                    users: [User!]!
                    messages: [ChatMessage!]!
                }
                
                type ChatMessage implements Node {
                    id: ID!
                    content: String!
                    time: Date!
                    user: User!
                }
            """.trimIndent())

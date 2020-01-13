import graphql.language.ObjectTypeDefinition
import graphql.parser.Parser
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.stream.Collectors

class GraphQLFile(
        private val file: File
) {
    private val parser = GraphQLFileParser()

    fun name(): String {
        return file.name
    }

    fun readText(): String {
        return file.bufferedReader().readText()
    }

    fun generateFileName(): String {
        return file.name.replace(".graphqls", "").replace(".graphql", "").capitalize()
    }

    fun extractObjectTypeData(): List<ObjectTypeData> {
        val schemaString = InputStreamReader(file.inputStream()).buffered().readText()
        return parser.parseObjectTypeDocument(schemaString)
    }
}

class GraphQLFileParser {
    fun parseObjectTypeDocument(schemaString: String): List<ObjectTypeData> {
        val document = Parser().parseDocument(schemaString)
        return document.definitions.filterIsInstance<ObjectTypeDefinition>()
                .filterNot { it.name == "Query" }
                .map { ObjectTypeData.from(it) }
    }
}

class GraphQLFileReader {
    fun read(path: String): List<GraphQLFile> {
        val fullPath = "${System.getProperty("user.dir")}/$path"
        val dest = File(fullPath)
        if (!dest.exists()) {
            throw IllegalStateException("schema file is not exist. path=$fullPath")
        }

        return if (dest.isDirectory) {
            Files.list(dest.toPath()).map { it.toFile() }.map { GraphQLFile(it) }.collect(Collectors.toList())
        } else {
            listOf(GraphQLFile(dest))
        }
    }
}

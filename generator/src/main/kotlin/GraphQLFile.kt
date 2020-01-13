import graphql.language.ObjectTypeDefinition
import graphql.parser.Parser
import java.io.File
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

    fun kotlinClassFileName(): String {
        val withoutExt = name().replace(".graphqls", "").replace(".graphql", "")
        return withoutExt.split("-", "_").joinToString("") { it.capitalize() }
    }

    fun extractObjectTypeData(): List<ObjectTypeData> {
        return parser.parseObjectTypeDocument(readText())
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

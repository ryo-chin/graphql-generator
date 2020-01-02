import graphql.language.ObjectTypeDefinition
import graphql.parser.Parser
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.file.Files

/**
 * @author hakiba
 */

fun main(args: Array<String>) {
    val generator = Generator()
    val schemaPath = args.asList().elementAtOrNull(0) ?: throw IllegalArgumentException("arg[0] schemaPath is required")
    val outputDirPath = args.asList().elementAtOrNull(1)
            ?: throw IllegalArgumentException("arg[1] outputDirPath is required")
    val schemaFile = generator.read(schemaPath)
            ?: throw IllegalStateException("schema file is not exist. schemaPath=$schemaPath")
    generator.parse(InputStreamReader(schemaFile.inputStream()).buffered().readText())
            .forEach { generator.generate(outputDirPath, it) }
}

class Generator {
    fun read(path: String): File? {
        return File("${System.getProperty("user.dir")}/$path")
    }

    fun parse(schemaString: String): List<TypeSchema> {
        val document = Parser().parseDocument(schemaString)
        return document.definitions.filterIsInstance<ObjectTypeDefinition>()
                .filterNot { it.name == "Query" }
                .map { TypeSchema.from(it) }
    }

    fun generate(outputDirPath: String, schema: TypeSchema) {
        val dir = File(outputDirPath)
        if (!dir.exists()) {
            Files.createDirectories(dir.toPath())
        }
        val writer = OutputStreamWriter(File("$outputDirPath/${schema.name}.kt").outputStream()).buffered()
        writer.write(schema.convertBody())
        writer.flush()
        writer.close()
    }
}

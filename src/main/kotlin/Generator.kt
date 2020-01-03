import graphql.language.ObjectTypeDefinition
import graphql.parser.Parser
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.util.stream.Collectors

/**
 * @author hakiba
 */

fun main(args: Array<String>) {
    val schemaPath = args.asList().elementAtOrNull(0) ?: throw IllegalArgumentException("arg[0] schemaPath is required")
    val outputDirPath = args.asList().elementAtOrNull(1)
            ?: throw IllegalArgumentException("arg[1] outputDirPath is required")
    val idType = System.getProperty("idType")?.let { IDType.valueOf(it) } ?: IDType.String

    val generator = Generator(idType)

    val schemaFile = generator.read(schemaPath).first()
    if (!schemaFile.exists()) {
        throw IllegalStateException("schema file is not exist. schemaPath=$schemaPath")
    }
    generator.parse(InputStreamReader(schemaFile.inputStream()).buffered().readText())
            .forEach { generator.generate(outputDirPath, it) }
}

class Generator(
        private val idType: IDType = IDType.String
) {

    fun read(path: String): List<File> {
        val dest = File("${System.getProperty("user.dir")}/$path")
        return if (dest.isDirectory) {
            Files.list(dest.toPath()).map { it.toFile() }.collect(Collectors.toList())
        } else {
            listOf(dest)
        }
    }

    fun parse(schemaString: String): List<ObjectTypeData> {
        val document = Parser().parseDocument(schemaString)
        return document.definitions.filterIsInstance<ObjectTypeDefinition>()
                .filterNot { it.name == "Query" }
                .map { ObjectTypeData.from(it) }
    }

    fun generate(outputDirPath: String, data: ObjectTypeData) {
        val dir = File(outputDirPath)
        if (!dir.exists()) {
            Files.createDirectories(dir.toPath())
        }
        val writer = OutputStreamWriter(File("$outputDirPath/${data.name}.kt").outputStream()).buffered()
        writer.write(data.convertBody(idType))
        writer.flush()
        writer.close()
    }
}

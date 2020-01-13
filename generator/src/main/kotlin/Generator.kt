import java.io.File
import java.io.OutputStreamWriter
import java.nio.file.Files

/**
 * @author hakiba
 */
fun main(args: Array<String>) {
    val schemaPath = args.asList().elementAtOrNull(0) ?: throw IllegalArgumentException("arg[0] schemaPath is required")
    val outputDirPath = args.asList().elementAtOrNull(1)
            ?: throw IllegalArgumentException("arg[1] outputDirPath is required")
    val idType = System.getProperty("idType")?.let { IDType.valueOf(it) } ?: IDType.String
    val outputMode = System.getProperty("outputMode")?.let { OutputMode.valueOf(it) } ?: OutputMode.Separate

    val generator = Generator(idType)
    val reader = GraphQLFileReader()
    val graphqlFiles = reader.read(schemaPath)

    when (outputMode) {
        OutputMode.Separate -> {
            graphqlFiles
                    .flatMap { it.extractObjectTypeData() }
                    .forEach { generator.generate(outputDirPath, it) }
        }
        OutputMode.OneToOne -> {
            graphqlFiles
                    .forEach { generator.generate(outputDirPath, it) }
        }
    }
}

class Generator(
        private val idType: IDType = IDType.String
) {

    fun generate(outputDirPath: String, data: ObjectTypeData) {
        val fileName = data.name
        val body = data.convertBody(idType)

        doGenerate(outputDirPath, fileName, body)
    }

    fun generate(outputDirPath: String, file: GraphQLFile) {
        val fileName = file.kotlinClassFileName()
        val body = file.extractObjectTypeData().joinToString(separator = "\n") { it.convertBody(idType) }

        doGenerate(outputDirPath, fileName, body)
    }

    private fun doGenerate(outputDirPath: String, fileName: String, body: String) {
        val dir = File(outputDirPath)
        if (!dir.exists()) {
            Files.createDirectories(dir.toPath())
        }
        val writer = OutputStreamWriter(File("$outputDirPath/$fileName.kt").outputStream()).buffered()
        parsePackageName(outputDirPath)?.let {
            writer.write("package $it\n")
            writer.newLine()
        }
        writer.write(body)
        writer.flush()
        writer.close()
    }

    fun parsePackageName(outputDirPath: String): String? {
        val escaped = if (outputDirPath.last() == '/') outputDirPath.substringBeforeLast("/") else outputDirPath
        return listOf("src/main/kotlin/", "src/test/kotlin/")
                .filter { escaped.contains(it) }
                .map { escaped.split(it).elementAt(1) }
                .map { it.replace("/", ".") }
                .firstOrNull()
    }
}

enum class OutputMode {
    Separate,
    OneToOne;
}

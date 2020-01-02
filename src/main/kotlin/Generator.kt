import graphql.language.FieldDefinition
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import graphql.parser.Parser
import java.io.File
import java.io.OutputStreamWriter
import java.nio.file.Files

/**
 * @author hakiba
 */
class Generator {
    fun read(path: String): File? {
        return File("${System.getProperty("user.dir")}/$path")
    }

    fun parse(schemaString: String): List<TypeSchema> {
        val document = Parser().parseDocument(schemaString)
        return document.definitions.filterIsInstance<ObjectTypeDefinition>()
                .filterNot { it.name == "Query" }
                .map { objType ->
                    TypeSchema(objType.name, objType.fieldDefinitions.map {
                        FieldData(it.name, typeName(it))
                    })
                }
    }

    private fun typeName(def: FieldDefinition): String {
        val typeName = when (def.type::class) {
            NonNullType::class -> {
                val nonNullType = def.type as NonNullType
                val typeName = nonNullType.type as TypeName
                typeName.name
            }
            TypeName::class -> {
                val nullableType = def.type as TypeName
                nullableType.name + "?"
            }
            else -> throw IllegalArgumentException("Illegal TypeName: type=${def.type}")
        }
        return when (typeName) {
            "ID" -> "String"
            else -> typeName
        }
    }

    fun convertBody(schema: TypeSchema): String {
        return """
            | data class ${schema.name}(
            | ${multiLine(schema.fields) { "    val ${it.name}: ${it.type}" }}
            | )
            | """.trimMargin("| ")
    }

    private fun <T: Any> multiLine(values: List<T>, convertFunc: (T) -> String): String {
        return values.joinToString(",\n", transform = convertFunc)
    }

    fun generate(outputDirPath: String, schema: TypeSchema) {
        val dir = File(outputDirPath)
        if (!dir.exists()) {
            Files.createDirectories(dir.toPath())
        }
        val writer = OutputStreamWriter(File("$outputDirPath/${schema.name}.kt").outputStream()).buffered()
        writer.write(convertBody(schema))
        writer.flush()
        writer.close()
    }
}

import graphql.language.FieldDefinition
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import graphql.parser.Parser
import java.io.File

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
}

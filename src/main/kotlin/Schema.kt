
import graphql.language.FieldDefinition
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName

class TypeSchema(
        val name: String,
        val fields: List<FieldData>
) {
    companion object {
        fun from(def: ObjectTypeDefinition): TypeSchema =
                TypeSchema(def.name, def.fieldDefinitions.map {
                    FieldData(it.name, typeName(it))
                })

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

    fun convertBody(): String {
        return """
            | data class $name(
            | ${multiLine(fields) { "    val ${it.name}: ${it.type}" }}
            | )
            | """.trimMargin("| ")
    }

    private fun <T : Any> multiLine(values: List<T>, convertFunc: (T) -> String): String {
        return values.joinToString(",\n", transform = convertFunc)
    }
}

class FieldData(
        val name: String,
        val type: String
)

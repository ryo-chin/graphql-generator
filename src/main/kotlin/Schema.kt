import graphql.language.*

class ObjectTypeData(
        val name: String,
        val fields: List<FieldData>
) {
    companion object {
        fun from(def: ObjectTypeDefinition): ObjectTypeData = ObjectTypeData(def.name, FieldData.from(def.fieldDefinitions))
    }

    fun convertBody(): String =
            """
            | data class $name(
            | ${multiLine(fields)
            { "    val ${it.name}: ${it.type}" }}
            | )
            | """.trimMargin("| ")

    private fun <T : Any> multiLine(values: List<T>, convertFunc: (T) -> String): String {
        return values.joinToString(",\n", transform = convertFunc)
    }
}

class FieldData(
        val name: String,
        val type: String
) {
    companion object {
        fun from(defs: List<FieldDefinition>): List<FieldData> = defs.map { FieldData(it.name, typeName(it.type)) }

        private fun typeName(type: Type, parent: Type? = null): String {
            val typeName = when (type) {
                is TypeName -> if (type.name == "ID") "String" else type.name
                is NonNullType -> typeName(type.type, type)
                is ListType -> "List<${typeName(type.type)}>"
                else -> throw IllegalArgumentException("Illegal Type: type=$type")
            }
            return if (type is NonNullType || parent is NonNullType) typeName else "$typeName?"
        }
    }
}

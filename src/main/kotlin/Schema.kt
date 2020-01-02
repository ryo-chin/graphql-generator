import graphql.language.*

class ObjectTypeData(
        val name: String,
        val fields: List<FieldData>
) {
    companion object {
        fun from(def: ObjectTypeDefinition): ObjectTypeData = ObjectTypeData(def.name, FieldData.from(def.fieldDefinitions))
    }

    fun convertBody(idType: IDType = IDType.String): String =
            """
            | data class $name(
            | ${multiLine(fields)
            { "    val ${it.name}: ${convertIdTypeIfNeed(it, idType)}" }}
            | )
            | """.trimMargin("| ")

    private fun convertIdTypeIfNeed(data: FieldData, idType: IDType): String {
        val idTypeName = idType.name
        return when {
            data.type.contains("<ID>") -> data.type.replace("<ID>", "<$idTypeName>")
            data.type.contains("ID?") -> data.type.replace("ID?", "$idTypeName?")
            data.type == "ID" -> idTypeName
            else -> data.type
        }
    }

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
                is TypeName -> type.name
                is NonNullType -> typeName(type.type, type)
                is ListType -> "List<${typeName(type.type)}>"
                else -> throw IllegalArgumentException("Illegal Type: type=$type")
            }
            return if (type is NonNullType || parent is NonNullType) typeName else "$typeName?"
        }
    }
}

enum class IDType {
    String,
    Long,
    Integer;
}

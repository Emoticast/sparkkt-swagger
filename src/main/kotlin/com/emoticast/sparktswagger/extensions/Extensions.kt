package com.emoticast.sparktswagger.extensions

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.emoticast.sparktswagger.Sealed
import java.io.StringReader
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

fun <T : Any?> T.print(): T = this.apply {
    val stackFrame = Thread.currentThread().stackTrace[2]
    val className = stackFrame.className
    val methodName = stackFrame.methodName
    val fileName = stackFrame.fileName
    val lineNumber = stackFrame.lineNumber
    println("$this at $className.$methodName($fileName:$lineNumber)")
}

val myConverter = object <T : Sealed> : Converter {
    override fun canConvert(cls: Class<*>) = Sealed::class.java.isAssignableFrom(cls)

    override fun toJson(value: Any): String = Klaxon().toJsonString(value)

    override fun fromJson(jv: JsonValue): T {
        val stringRep = jv.obj?.toJsonString()
        val clazz = jv.propertyKClass?.jvmErasure!!
        val subtype = if (clazz.isFinal) clazz else {
            val nestedClasses = clazz.nestedClasses
            val subclasses = nestedClasses.filter { it.isFinal && it.isSubclassOf(clazz) }
            val type = jv.objString(Sealed::type.name)
            subclasses.find { it.simpleName == type }
        }
        return Klaxon().let { it.fromJsonObject(it.parser(subtype).parse(StringReader(stringRep)) as JsonObject, subtype!!.java, subtype!!)!! as T }
    }
}

val klaxon = Klaxon().converter(myConverter)
val Any.json: String get() = klaxon.toJsonString(this)
inline fun <reified T> T.toHashMap() = (klaxon.parser(T::class).parse(StringReader(this?.json)) as JsonObject).map

inline fun <reified T : Any> String.parseJson(): T = try {
    klaxon.parse<T>(this)!!
} catch (t: Throwable) {
   throw ParsingException("Error parsing $this to ${T::class.simpleName}(${T::class.primaryConstructor?.parameters?.joinToString(",") { "${it.name }: ${it.type}"}})", t)
}

fun <T : Any> Klaxon.parse(json: String, `class`: KClass<T>): T? = fromJsonObject(parser(`class`).parse(StringReader(json)) as JsonObject, `class`.java, `class`) as T
data class ParsingException(val rawJson: String, val error: Throwable) : RuntimeException(rawJson, error)

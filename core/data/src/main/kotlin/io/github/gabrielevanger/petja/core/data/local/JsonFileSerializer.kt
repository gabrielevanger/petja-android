package io.github.gabrielevanger.petja.core.data.local

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Grava um objeto como JSON num arquivo do DataStore. Campo novo ou removido não quebra o arquivo antigo. */
internal class JsonFileSerializer<T>(private val serializer: KSerializer<T>, override val defaultValue: T) :
    Serializer<T> {

    override suspend fun readFrom(input: InputStream): T = try {
        json.decodeFromString(serializer, input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Arquivo local ilegível", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Arquivo local ilegível", e)
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(json.encodeToString(serializer, t).encodeToByteArray())
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

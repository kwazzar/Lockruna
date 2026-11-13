package com.kwazzar.lockruna.data.db

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.vault.VaultItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Decrypted item content; everything here lives inside the ciphertext on disk (ADR-005). */
@Serializable
data class VaultItemContent(
    @SerialName("title") val title: String,
    @SerialName("username") val username: String?,
    @SerialName("secret") val secret: String?,
    @SerialName("url") val url: String?,
    @SerialName("folderId") val folderId: String?,
    @SerialName("cardNumber") val cardNumber: String? = null,
    @SerialName("cardExpiry") val cardExpiry: String? = null,
    @SerialName("cardCvv") val cardCvv: String? = null,
    @SerialName("noteBody") val noteBody: String? = null,
) {
    override fun toString(): String =
        "VaultItemContent(title=len=${title.length}, username=${username?.length}, " +
            "secret=${secret?.length}, url=${url?.length}, folderId=${folderId?.length}, " +
            "cardNumber=${cardNumber?.length}, cardExpiry=${cardExpiry?.length}, " +
            "cardCvv=${cardCvv?.length}, noteBody=${noteBody?.length})"
}

/** Maps a `vault_item` row to the domain aggregate, sealing the content into JSON for encryption. */
object VaultItemRowMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun toContent(item: VaultItem): VaultItemContent = VaultItemContent(
        title = item.title.value,
        username = item.username,
        secret = item.secret?.value,
        url = item.url?.value,
        folderId = item.folderId?.value,
        cardNumber = item.cardNumber?.value,
        cardExpiry = item.cardExpiry,
        cardCvv = item.cardCvv?.value,
        noteBody = item.noteBody?.value,
    )

    fun serializeContent(content: VaultItemContent): String = json.encodeToString(content)

    fun deserializeContent(json: String): Result<VaultItemContent, AppError> = try {
        Result.Success(this.json.decodeFromString<VaultItemContent>(json))
    } catch (ignored: SerializationException) {
        // SerializationException extends IllegalArgumentException, so it also covers garbage from a wrong key.
        Result.Failure(AppError.ItemCorrupted)
    }
}

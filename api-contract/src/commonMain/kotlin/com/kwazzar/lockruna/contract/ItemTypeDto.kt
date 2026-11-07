package com.kwazzar.lockruna.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Vault item type as defined by `openapi.yaml` (`ItemType`). */
@Serializable
enum class ItemTypeDto {
    @SerialName("login")
    LOGIN,

    @SerialName("card")
    CARD,

    @SerialName("note")
    NOTE,

    @SerialName("folder")
    FOLDER,
}

package com.haertibraeu.hopledger.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Responses ---
@Serializable
data class HealthResponse(val status: String, val service: String, val database: String)

// --- Domain Models ---
@Serializable
data class Brewer(val id: String, val name: String)

@Serializable
data class Beer(val id: String, val name: String, val style: String? = null, @SerialName("batchId") val batchId: String? = null)

@Serializable
data class Location(val id: String, val name: String, val type: String = "general", @SerialName("brewerId") val brewerId: String? = null)

@Serializable
data class ContainerType(
    val id: String,
    val name: String,
    val icon: String? = null,
    @SerialName("externalPrice") val externalPrice: Double = 0.0,
    @SerialName("internalPrice") val internalPrice: Double = 0.0,
    @SerialName("depositFee") val depositFee: Double = 0.0,
)

@Serializable
data class Container(
    val id: String,
    @SerialName("containerTypeId") val containerTypeId: String,
    val containerType: ContainerType? = null,
    @SerialName("beerId") val beerId: String? = null,
    val beer: Beer? = null,
    @SerialName("locationId") val locationId: String,
    val location: Location? = null,
    @SerialName("isEmpty") val isEmpty: Boolean = true,
    @SerialName("isReserved") val isReserved: Boolean = false,
    @SerialName("reservedFor") val reservedFor: String? = null,
)

@Serializable
data class Balance(
    @SerialName("brewerId") val brewerId: String,
    @SerialName("brewerName") val brewerName: String,
    val balance: Double,
)

@Serializable
data class AccountEntry(
    val id: String,
    @SerialName("brewerId") val brewerId: String,
    val brewer: Brewer? = null,
    val amount: Double,
    val type: String,
    val description: String? = null,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
data class EntriesResponse(
    val entries: List<AccountEntry>,
    val pagination: Pagination,
)

@Serializable
data class Pagination(val page: Int, val limit: Int, val total: Int, val pages: Int)

@Serializable
data class Settlement(
    val from: SettlementParty,
    val to: SettlementParty,
    val amount: Double,
)

@Serializable
data class SettlementParty(val id: String, val name: String)

@Serializable
data class ActionResult(val container: Container, val accountEntry: AccountEntry)

// --- Request Bodies ---
@Serializable
data class BrewerRequest(val name: String)

@Serializable
data class BeerRequest(val name: String, val style: String? = null, val batchId: String? = null)

@Serializable
data class LocationRequest(val name: String, val type: String = "general", val brewerId: String? = null)

@Serializable
data class ContainerTypeRequest(
    val name: String,
    val icon: String? = null,
    val externalPrice: Double = 0.0,
    val internalPrice: Double = 0.0,
    val depositFee: Double = 0.0,
)

@Serializable
data class ContainerCreateRequest(val containerTypeId: String, val locationId: String, val beerId: String? = null)

@Serializable
data class MoveRequest(val locationId: String)

@Serializable
data class FillRequest(val beerId: String)

@Serializable
data class ReserveRequest(val reservedFor: String)

@Serializable
data class BatchFillRequest(val containerIds: List<String>, val beerId: String)

@Serializable
data class EntryRequest(val brewerId: String, val amount: Double, val type: String = "manual", val description: String? = null)

@Serializable
data class SellRequest(val containerId: String, val brewerId: String, val customerLocationId: String)

@Serializable
data class SelfConsumeRequest(val containerId: String, val brewerId: String)

@Serializable
data class ContainerReturnRequest(val containerId: String, val brewerId: String, val returnLocationId: String)

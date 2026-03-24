package com.haertibraeu.hopledger.data.api

import com.haertibraeu.hopledger.data.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface HopLedgerApi {
    // Health
    @GET("api/health")
    suspend fun health(): HealthResponse

    // Brewers
    @GET("api/brewers")
    suspend fun getBrewers(): List<Brewer>
    @POST("api/brewers")
    suspend fun createBrewer(@Body body: BrewerRequest): Brewer
    @PUT("api/brewers/{id}")
    suspend fun updateBrewer(@Path("id") id: String, @Body body: BrewerRequest): Brewer
    @DELETE("api/brewers/{id}")
    suspend fun deleteBrewer(@Path("id") id: String)

    // Beers
    @GET("api/beers")
    suspend fun getBeers(): List<Beer>
    @POST("api/beers")
    suspend fun createBeer(@Body body: BeerRequest): Beer
    @PUT("api/beers/{id}")
    suspend fun updateBeer(@Path("id") id: String, @Body body: BeerRequest): Beer
    @DELETE("api/beers/{id}")
    suspend fun deleteBeer(@Path("id") id: String)

    // Locations
    @GET("api/locations")
    suspend fun getLocations(): List<Location>
    @POST("api/locations")
    suspend fun createLocation(@Body body: LocationRequest): Location
    @PUT("api/locations/{id}")
    suspend fun updateLocation(@Path("id") id: String, @Body body: LocationRequest): Location
    @DELETE("api/locations/{id}")
    suspend fun deleteLocation(@Path("id") id: String)

    // Container Types
    @GET("api/container-types")
    suspend fun getContainerTypes(): List<ContainerType>
    @POST("api/container-types")
    suspend fun createContainerType(@Body body: ContainerTypeRequest): ContainerType
    @PUT("api/container-types/{id}")
    suspend fun updateContainerType(@Path("id") id: String, @Body body: ContainerTypeRequest): ContainerType
    @DELETE("api/container-types/{id}")
    suspend fun deleteContainerType(@Path("id") id: String)

    // Containers
    @GET("api/containers")
    suspend fun getContainers(
        @Query("locationId") locationId: String? = null,
        @Query("beerId") beerId: String? = null,
        @Query("containerTypeId") containerTypeId: String? = null,
        @Query("isEmpty") isEmpty: Boolean? = null,
        @Query("isReserved") isReserved: Boolean? = null,
    ): List<Container>
    @POST("api/containers")
    suspend fun createContainer(@Body body: ContainerCreateRequest): Container
    @DELETE("api/containers/{id}")
    suspend fun deleteContainer(@Path("id") id: String)
    @POST("api/containers/{id}/move")
    suspend fun moveContainer(@Path("id") id: String, @Body body: MoveRequest): Container
    @POST("api/containers/{id}/fill")
    suspend fun fillContainer(@Path("id") id: String, @Body body: FillRequest): Container
    @POST("api/containers/{id}/destroy-beer")
    suspend fun destroyBeer(@Path("id") id: String): Container
    @POST("api/containers/{id}/reserve")
    suspend fun reserveContainer(@Path("id") id: String, @Body body: ReserveRequest): Container
    @POST("api/containers/{id}/unreserve")
    suspend fun unreserveContainer(@Path("id") id: String): Container
    @POST("api/containers/batch-fill")
    suspend fun batchFill(@Body body: BatchFillRequest): List<Container>

    // Accounting
    @GET("api/accounting/balances")
    suspend fun getBalances(): List<Balance>
    @GET("api/accounting/entries")
    suspend fun getEntries(
        @Query("brewerId") brewerId: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): EntriesResponse
    @POST("api/accounting/entries")
    suspend fun createEntry(@Body body: EntryRequest): AccountEntry
    @DELETE("api/accounting/entries/{id}")
    suspend fun deleteEntry(@Path("id") id: String)
    @GET("api/accounting/settlements")
    suspend fun getSettlements(): List<Settlement>

    // Categories
    @GET("api/categories")
    suspend fun getCategories(): List<Category>
    @POST("api/categories")
    suspend fun createCategory(@Body body: CategoryRequest): Category
    @PUT("api/categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body body: CategoryRequest): Category
    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String)

    // Combined Actions
    @POST("api/actions/sell")
    suspend fun sell(@Body body: SellRequest): ActionResult
    @POST("api/actions/batch-sell")
    suspend fun batchSell(@Body body: BatchSellRequest): BatchActionResult
    @POST("api/actions/self-consume")
    suspend fun selfConsume(@Body body: SelfConsumeRequest): ActionResult
    @POST("api/actions/container-return")
    suspend fun containerReturn(@Body body: ContainerReturnRequest): ActionResult

    // Backup
    @Streaming
    @GET("api/backup/export")
    suspend fun exportBackup(): retrofit2.Response<okhttp3.ResponseBody>

    @Multipart
    @POST("api/backup/import")
    suspend fun importBackup(@Part backup: okhttp3.MultipartBody.Part): retrofit2.Response<okhttp3.ResponseBody>
}

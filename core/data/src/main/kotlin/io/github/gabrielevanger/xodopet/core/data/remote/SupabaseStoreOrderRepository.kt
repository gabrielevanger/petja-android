package io.github.gabrielevanger.xodopet.core.data.remote

import io.github.gabrielevanger.xodopet.core.data.remote.dto.STORE_ORDER_COLUMNS
import io.github.gabrielevanger.xodopet.core.data.remote.dto.StoreMembershipDto
import io.github.gabrielevanger.xodopet.core.data.remote.dto.StoreOrderDto
import io.github.gabrielevanger.xodopet.core.domain.model.Money
import io.github.gabrielevanger.xodopet.core.domain.model.OrderAddress
import io.github.gabrielevanger.xodopet.core.domain.model.OrderItem
import io.github.gabrielevanger.xodopet.core.domain.model.OrderStatus
import io.github.gabrielevanger.xodopet.core.domain.storemode.ManagedStore
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrder
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrderError
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrderException
import io.github.gabrielevanger.xodopet.core.domain.storemode.StoreOrderRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** SQLSTATE de "sem permissão": chamada sem login. */
private const val PERMISSION_DENIED = "42501"

/** Os encerrados ficam na tela por um dia, para a loja conferir o que já saiu. */
private val FINISHED_WINDOW: Duration = Duration.ofHours(24)

private val ACTIVE_STATUSES = listOf(
    OrderStatus.Pending,
    OrderStatus.Accepted,
    OrderStatus.Preparing,
    OrderStatus.OutForDelivery,
)

/**
 * Pedidos da loja pela leitura direta das tabelas (a RLS só mostra as lojas que a conta opera) e as
 * mudanças de etapa pela função transition_order, que confere a máquina de estados no servidor.
 */
internal class SupabaseStoreOrderRepository(private val supabase: SupabaseClient, private val clock: Clock) :
    StoreOrderRepository {

    @Inject
    constructor(supabase: SupabaseClient) : this(supabase, Clock.systemUTC())

    /** Dono e atendente operam a loja; entregador tem o modo dele. Várias linhas da mesma loja viram uma. */
    override suspend fun managedStores(): List<ManagedStore> = translateErrors {
        supabase.from("store_members")
            .select(Columns.raw("store:stores(id,name)")) { filter { isIn("role", listOf("owner", "staff")) } }
            .decodeList<StoreMembershipDto>()
            .mapNotNull { it.store }
            .distinctBy { it.id }
            .map { ManagedStore(it.id, it.name) }
            .sortedBy { it.name }
    }

    override suspend fun orders(storeId: String): List<StoreOrder> = translateErrors {
        val since = clock.instant().minus(FINISHED_WINDOW)
        supabase.from("orders")
            .select(Columns.raw(STORE_ORDER_COLUMNS)) {
                filter {
                    eq("store_id", storeId)
                    or {
                        isIn("status", ACTIVE_STATUSES.map { it.toDbValue() })
                        gte("created_at", since.toString())
                    }
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<StoreOrderDto>()
            .map { it.toDomain() }
    }

    override suspend fun transition(orderId: String, to: OrderStatus, reason: String?) = translateErrors {
        val parameters = buildJsonObject {
            put("p_order_id", orderId)
            put("p_to", to.toDbValue())
            reason?.takeIf { it.isNotBlank() }?.let { put("p_reason", it) }
        }
        supabase.postgrest.rpc("transition_order", parameters)
        Unit
    }

    /** Um canal por loja, aberto só enquanto a tela escuta; pega pedido novo e mudança de etapa. */
    override fun changes(storeId: String): Flow<Unit> = flow {
        val channel = supabase.channel("store-orders-$storeId")
        val events = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "orders"
            filter("store_id", FilterOperator.EQ, storeId)
        }
        try {
            channel.subscribe()
            emitAll(events.map { })
        } finally {
            withContext(NonCancellable) { supabase.realtime.removeChannel(channel) }
        }
    }

    private suspend fun <T> translateErrors(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: PostgrestRestException) {
        val error = when {
            e.code == PERMISSION_DENIED || e.error == "not_authenticated" -> StoreOrderError.NotSignedIn
            e.error == "forbidden" -> StoreOrderError.NotAllowed
            e.error == "invalid_transition" -> StoreOrderError.InvalidTransition
            e.error == "reason_required" -> StoreOrderError.ReasonRequired
            else -> StoreOrderError.Unknown
        }
        throw StoreOrderException(error, e)
    } catch (e: HttpRequestException) {
        throw StoreOrderException(StoreOrderError.Network, e)
    } catch (e: HttpRequestTimeoutException) {
        throw StoreOrderException(StoreOrderError.Network, e)
    } catch (e: Exception) {
        throw StoreOrderException(StoreOrderError.Unknown, e)
    }
}

internal fun StoreOrderDto.toDomain() = StoreOrder(
    id = id,
    number = number,
    status = parseStatus(status),
    statusReason = statusReason,
    customerName = customerName,
    customerPhone = customerPhone?.takeIf { it.isNotBlank() },
    items = items.map {
        OrderItem(it.variantId, it.productName, it.variantLabel, Money(it.unitPriceCents), it.quantity)
    },
    total = Money(totalCents),
    payment = parsePayment(paymentMethod, changeForCents),
    notes = notes?.takeIf { it.isNotBlank() },
    address = OrderAddress(street, numberAddress, complement, neighborhood, reference),
    createdAt = parseTimestamp(createdAt),
)

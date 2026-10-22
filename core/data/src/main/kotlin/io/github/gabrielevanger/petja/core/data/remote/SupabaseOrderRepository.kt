package io.github.gabrielevanger.petja.core.data.remote

import io.github.gabrielevanger.petja.core.data.remote.dto.ORDER_DETAILS_COLUMNS
import io.github.gabrielevanger.petja.core.data.remote.dto.ORDER_SUMMARY_COLUMNS
import io.github.gabrielevanger.petja.core.data.remote.dto.OrderDetailsDto
import io.github.gabrielevanger.petja.core.data.remote.dto.OrderSummaryDto
import io.github.gabrielevanger.petja.core.domain.model.OrderDetails
import io.github.gabrielevanger.petja.core.domain.model.OrderSummary
import io.github.gabrielevanger.petja.core.domain.orders.OrderError
import io.github.gabrielevanger.petja.core.domain.orders.OrderException
import io.github.gabrielevanger.petja.core.domain.orders.OrderRepository
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

/** SQLSTATE de "sem permissão": usuário sem login chamando uma função de pedido. */
private const val PERMISSION_DENIED = "42501"

/**
 * Pedidos do cliente pela leitura direta das tabelas (a RLS só mostra os dele) e cancelamento pela
 * função transition_order, que confere a máquina de estados no servidor.
 */
internal class SupabaseOrderRepository @Inject constructor(private val supabase: SupabaseClient) : OrderRepository {

    override suspend fun myOrders(): List<OrderSummary> = translateErrors {
        supabase.from("orders")
            .select(Columns.raw(ORDER_SUMMARY_COLUMNS)) { order("created_at", Order.DESCENDING) }
            .decodeList<OrderSummaryDto>()
            .map { it.toDomain() }
    }

    override suspend fun order(orderId: String): OrderDetails? = translateErrors {
        supabase.from("orders")
            .select(Columns.raw(ORDER_DETAILS_COLUMNS)) { filter { eq("id", orderId) } }
            .decodeList<OrderDetailsDto>()
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun cancel(orderId: String) = translateErrors {
        val parameters = buildJsonObject {
            put("p_order_id", orderId)
            put("p_to", "canceled")
        }
        supabase.postgrest.rpc("transition_order", parameters)
        Unit
    }

    /** Um canal por pedido, aberto só enquanto alguém escuta; a RLS também vale no Realtime. */
    override fun changes(orderId: String): Flow<Unit> = flow {
        val channel = supabase.channel("order-$orderId")
        val updates = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "orders"
            filter("id", FilterOperator.EQ, orderId)
        }
        try {
            channel.subscribe()
            emitAll(updates.map { })
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
            e.code == PERMISSION_DENIED || e.error == "not_authenticated" -> OrderError.NotSignedIn
            e.error == "invalid_transition" || e.error == "forbidden" -> OrderError.CannotCancel
            else -> OrderError.Unknown
        }
        throw OrderException(error, e)
    } catch (e: HttpRequestException) {
        throw OrderException(OrderError.Network, e)
    } catch (e: HttpRequestTimeoutException) {
        throw OrderException(OrderError.Network, e)
    } catch (e: Exception) {
        throw OrderException(OrderError.Unknown, e)
    }
}

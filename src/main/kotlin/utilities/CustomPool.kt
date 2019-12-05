package utilities

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinWorkerThread
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import com.uptake.omnibus.config

object CustomPool {
    var parallelism = 0

    val instance by lazy {
        val parallelismInit = if (parallelism > 0) parallelism
        else if (config.parallelism > 0) config.parallelism
        else (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(1)

        Pool(ForkJoinPool(parallelismInit))
    }
}

open class Pool(val pool: ForkJoinPool) : AbstractCoroutineContextElement(ContinuationInterceptor),
        ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
            PoolContinuation(pool, continuation.context.fold(continuation, { cont, element ->
                if (element != this@Pool && element is ContinuationInterceptor)
                    element.interceptContinuation(cont) else cont
            }))
}

private class PoolContinuation<T>(
        val pool: ForkJoinPool,
        val continuation: Continuation<T>
) : Continuation<T> by continuation {
    override fun resume(value: T) {
        if (isPoolThread()) continuation.resume(value)
        else pool.execute { continuation.resume(value) }
    }

    override fun resumeWithException(exception: Throwable) {
        if (isPoolThread()) continuation.resumeWithException(exception)
        else pool.execute { continuation.resumeWithException(exception) }
    }

    fun isPoolThread(): Boolean = (Thread.currentThread() as? ForkJoinWorkerThread)?.pool == pool
}

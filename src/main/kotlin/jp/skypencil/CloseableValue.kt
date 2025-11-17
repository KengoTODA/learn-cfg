package jp.skypencil

import org.objectweb.asm.tree.analysis.Value

/**
 * A stateless [Value] implementation that holds state of [AutoCloseable] instance.
 */
class CloseableValue(private val closed: Boolean) : Value {
    override fun getSize(): Int = 1

    fun isClosed(): Boolean = closed
}

package jp.skypencil

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.analysis.AnalyzerException
import org.objectweb.asm.tree.analysis.Interpreter

class CloseableInterpreter @JvmOverloads constructor(api: Int = Opcodes.ASM7) :
    Interpreter<CloseableValue>(api) {

    override fun newValue(type: Type?): CloseableValue? {
        return when {
            type == null -> UNINITIALIZED_VALUE
            type.sort == Type.VOID -> null
            type.sort != Type.OBJECT -> CloseableValue(true)
            else -> CloseableValue(false) // TODO confirm that object implements AutoCloseable
        }
    }

    @Throws(AnalyzerException::class)
    override fun newOperation(insn: AbstractInsnNode): CloseableValue? {
        val type: Type? = when (insn.opcode) {
            Opcodes.GETSTATIC -> Type.getType((insn as FieldInsnNode).desc) // assume that we do not store closed instance to field
            Opcodes.NEW -> Type.getObjectType((insn as TypeInsnNode).desc)
            else -> null
        }
        return newValue(type)
    }

    @Throws(AnalyzerException::class)
    override fun copyOperation(
        insn: AbstractInsnNode,
        value: CloseableValue,
    ): CloseableValue = value

    @Throws(AnalyzerException::class)
    override fun unaryOperation(
        insn: AbstractInsnNode,
        value: CloseableValue,
    ): CloseableValue = value // the CloseableValue is stateless, so we can reuse the given reference

    @Throws(AnalyzerException::class)
    override fun binaryOperation(
        insn: AbstractInsnNode,
        value1: CloseableValue,
        value2: CloseableValue,
    ): CloseableValue? {
        val type: Type? = when (insn.opcode) {
            Opcodes.GETFIELD -> Type.getType((insn as FieldInsnNode).desc) // assume that we do not store closed instance to field
            Opcodes.CHECKCAST -> Type.getObjectType((insn as TypeInsnNode).desc)
            else -> null
        }
        return newValue(type)
    }

    @Throws(AnalyzerException::class)
    override fun ternaryOperation(
        insn: AbstractInsnNode,
        value1: CloseableValue,
        value2: CloseableValue,
        value3: CloseableValue,
    ): CloseableValue? = null

    @Throws(AnalyzerException::class)
    override fun naryOperation(
        insn: AbstractInsnNode,
        values: List<CloseableValue>,
    ): CloseableValue? {
        val type: Type? = when (insn.opcode) {
            Opcodes.MULTIANEWARRAY -> null
            Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC, Opcodes.INVOKEINTERFACE -> {
                val methodInsn = insn as MethodInsnNode
                if ("close" == methodInsn.name && "()V" == methodInsn.desc) {
                    // TODO confirm that the target instance implements AutoCloseable
                    System.err.println("Wow closed!")
                    return CloseableValue(true)
                } else {
                    Type.getReturnType(methodInsn.desc)
                }
            }
            Opcodes.INVOKEDYNAMIC -> {
                val dynamicInsn = insn as InvokeDynamicInsnNode
                // TODO need to find a Handler from bsmArgs?
                val handle = dynamicInsn.bsmArgs[1] as Handle
                if ("close" == handle.name && "()V" == handle.desc) {
                    // TODO confirm that the target instance implements AutoCloseable
                    System.err.println("Wow closed!")
                    return CloseableValue(true)
                } else {
                    Type.getReturnType(dynamicInsn.desc)
                }
            }
            else -> null
        }
        return newValue(type)
    }

    @Throws(AnalyzerException::class)
    override fun returnOperation(
        insn: AbstractInsnNode,
        value: CloseableValue,
        expected: CloseableValue,
    ) {
        // No-op
    }

    override fun merge(value1: CloseableValue, value2: CloseableValue): CloseableValue {
        return CloseableValue(value1.isClosed() && value2.isClosed())
    }

    companion object {
        private val UNINITIALIZED_VALUE = CloseableValue(true)
    }
}

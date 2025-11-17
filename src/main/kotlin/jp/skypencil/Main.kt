package jp.skypencil

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class Main {
    init {
        val o: Any? = if (System.currentTimeMillis() % 2 == 0L) {
            println("0")
            null
        } else {
            println("1")
            Any()
        }
        if (o != null) println(o)
        val a = Runnable { System.out.close() }
        a.run()
    }

    companion object {
        private const val DEFAULT =
            "/Users/kengo/GitHub/spotbugs/spotbugsTestCases/build/classes/java/java11/Issue1338.class"

        @JvmStatic
        @Throws(IOException::class)
        fun main(vararg args: String) {
            val classFile = if (args.isEmpty()) DEFAULT else args[0]
            Files.newInputStream(Paths.get(classFile)).use { input ->
                val classReader = ClassReader(input)
                val classNode = ClassNode()
                classReader.accept(classNode, ClassReader.SKIP_DEBUG)

                println("digraph ${classNode.name.replace('/', '_')} {")
                var methodIndex = 0
                for (method in classNode.methods) {
                    if (method.instructions.size() > 0) {
                        println(
                            "  subgraph cluster_method_$methodIndex {\n" +
                                "    label=\"#${method.name}\";\n" +
                                "    graph[style=dotted];",
                        )
                        val index = methodIndex
                        for (i in 0 until method.instructions.size()) {
                            val insn = method.instructions.get(i)
                            when (insn) {
                                is MethodInsnNode -> {
                                    println(
                                        "    edge${index}_$i [label=\"${insn.owner}#${insn.name}${insn.desc}\"];",
                                    )
                                }
                                is FieldInsnNode -> {
                                    println(
                                        "    edge${index}_$i [label=\"${insn.owner}#${insn.name} (${insn.desc})\"];",
                                    )
                                }
                                is VarInsnNode -> {
                                    val insnName = getName(insn.opcode)
                                    println("    edge${index}_$i [label=\"$insnName (${insn.`var`})\"];")
                                }
                                is InsnNode -> {
                                    val insnName = getName(insn.opcode)
                                    println("    edge${index}_$i [label=\"$insnName\"];")
                                }
                                else -> {
                                    println("    edge${index}_$i [label=\"${insn.javaClass}\"];")
                                }
                            }
                        }

                        if (true) {
                            val analyzer = object : Analyzer<BasicValue>(
                                object : BasicInterpreter(Opcodes.ASM7) {
                                    override fun merge(value1: BasicValue, value2: BasicValue): BasicValue {
                                        // if (!value1.equals(value2)) {
                                        //  System.err.printf("%s, %s%n", value1.getType(), value2.getType());
                                        // }
                                        return super.merge(value1, value2)
                                    }
                                },
                            ) {
                                override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
                                    val insn = method.instructions.get(insnIndex)
                                    val opcode = insn.opcode

                                    if (insn is JumpInsnNode) {
                                        val label = when (opcode) {
                                            Opcodes.IFNULL -> if (successorIndex == insnIndex + 1) "" else "if null"
                                            Opcodes.IFNONNULL -> if (successorIndex == insnIndex + 1) "" else "if not null"
                                            Opcodes.GOTO -> ""
                                            else -> "unknown ($opcode)"
                                        }
                                        println("    edge${index}_$insnIndex -> edge${index}_$successorIndex [label=\"$label\"];")
                                    } else {
                                        println("    edge${index}_$insnIndex -> edge${index}_$successorIndex;")
                                    }
                                }

                                override fun newControlFlowExceptionEdge(insnIndex: Int, successorIndex: Int): Boolean {
                                    println(
                                        "    edge${index}_$insnIndex -> edge${index}_$successorIndex [color = \"red\"];",
                                    )
                                    return true
                                }
                            }
                            analyzer.analyze(classNode.name, method)
                            println("  }")
                        } else {
                            val analyzer = Analyzer(CloseableInterpreter())
                            analyzer.analyze(classNode.name, method)
                            println("  }")
                        }
                    }
                    methodIndex++
                    // break
                }
                println("}")
            }
        }

        private fun getName(opcode: Int): String {
            return when (opcode) {
                Opcodes.ALOAD -> "ALOAD"
                Opcodes.ASTORE -> "ASTORE"
                Opcodes.RETURN -> "RETURN"
                Opcodes.ATHROW -> "ATHROW"
                else -> "unknown ($opcode)"
            }
        }
    }
}

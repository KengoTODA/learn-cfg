package jp.skypencil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

public class Main {
  private static final String DEFAULT =
      "/Users/kengo/GitHub/spotbugs/spotbugsTestCases/build/classes/java/java11/Issue1338.class";

  public Main() {
    Object o = null;
    if (System.currentTimeMillis() % 2 == 0) {
      System.out.println("0");
    } else {
      System.out.println("1");
      o = new Object();
    }
    if (o != null) System.out.println(o);
    Runnable a = System.out::close;
    a.run();
  }

  public static void main(String... args) throws IOException, AnalyzerException {
    String classFile = args.length == 0 ? DEFAULT : args[0];
    try (InputStream input = Files.newInputStream(Paths.get(classFile))) {
      ClassReader classReader = new ClassReader(input);
      ClassNode classNode = new ClassNode();
      classReader.accept(classNode, ClassReader.SKIP_DEBUG);

      System.out.printf("digraph %s {%n", classNode.name.replace('/', '_'));
      int methodIndex = 0;
      for (MethodNode method : classNode.methods) {
        if (method.instructions.size() > 0) {
          System.out.printf(
              "  subgraph cluster_method_%d {%n    label=\"#%s\";%n    graph[style=dotted];%n",
              methodIndex, method.name, method.name);
          final int index = methodIndex;
          for (int i = 0; i < method.instructions.size(); ++i) {
            AbstractInsnNode insn = method.instructions.get(i);
            if (insn instanceof MethodInsnNode) {
              MethodInsnNode methodInsn = (MethodInsnNode) insn;
              System.out.printf(
                  "    edge%d_%d [label=\"%s#%s%s\"];%n",
                  index, i, methodInsn.owner, methodInsn.name, methodInsn.desc);
            } else if (insn instanceof FieldInsnNode) {
              FieldInsnNode fieldInsn = (FieldInsnNode) insn;
              System.out.printf(
                  "    edge%d_%d [label=\"%s#%s (%s)\"];%n",
                  index, i, fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
            } else if (insn instanceof VarInsnNode) {
              VarInsnNode varInsn = (VarInsnNode) insn;
              String insnName = getName(varInsn.getOpcode());
              System.out.printf(
                  "    edge%d_%d [label=\"%s (%d)\"];%n", index, i, insnName, varInsn.var);
            } else if (insn instanceof InsnNode) {
              InsnNode insnNode = (InsnNode) insn;
              String insnName = getName(insnNode.getOpcode());
              System.out.printf("    edge%d_%d [label=\"%s\"];%n", index, i, insnName);
            } else {
              System.out.printf("    edge%d_%d [label=\"%s\"];%n", index, i, insn.getClass());
            }
          }

          if (true) {
            Analyzer<BasicValue> analyzer =
                new Analyzer<BasicValue>(
                    new BasicInterpreter(Opcodes.ASM7) {
                      @Override
                      public BasicValue merge(final BasicValue value1, final BasicValue value2) {
                        // if (!value1.equals(value2)) {
                        //  System.err.printf("%s, %s%n", value1.getType(), value2.getType());

                        // }
                        return super.merge(value1, value2);
                      }
                    }) {
                  @Override
                  protected void newControlFlowEdge(final int insnIndex, final int successorIndex) {
                    AbstractInsnNode insn = method.instructions.get(insnIndex);
                    int opcode = insn.getOpcode();

                    if (insn instanceof JumpInsnNode) {
                      String label = "unknown (" + opcode + ")";
                      switch (opcode) {
                        case Opcodes.IFNULL:
                          if (successorIndex == insnIndex + 1) {
                            label = "";
                          } else {
                            label = "if null";
                          }
                          break;
                        case Opcodes.IFNONNULL:
                          if (successorIndex == insnIndex + 1) {
                            label = "";
                          } else {
                            label = "if not null";
                          }
                          break;
                        case Opcodes.GOTO:
                          label = "";
                          break;
                      }
                      System.out.printf(
                          "    edge%d_%d -> edge%d_%d [label=\"%s\"];%n",
                          index, insnIndex, index, successorIndex, label);
                    } else {
                      System.out.printf(
                          "    edge%d_%d -> edge%d_%d;%n", index, insnIndex, index, successorIndex);
                    }
                  }

                  @Override
                  protected boolean newControlFlowExceptionEdge(
                      final int insnIndex, final int successorIndex) {
                    System.out.printf(
                        "    edge%d_%d -> edge%d_%d [color = \"red\"];%n",
                        index, insnIndex, index, successorIndex);
                    return true;
                  }
                };
            analyzer.analyze(classNode.name, method);
            System.out.println("  }");
          } else {
            Analyzer<CloseableValue> analyzer = new Analyzer<>(new CloseableInterpreter());
            analyzer.analyze(classNode.name, method);
            System.out.println("  }");
          }
        }
        methodIndex++;
        //        break;
      }
      System.out.println("}");
    }
  }

  private static String getName(int opcode) {
    switch (opcode) {
      case Opcodes.ALOAD:
        return "ALOAD";
      case Opcodes.ASTORE:
        return "ASTORE";
      case Opcodes.RETURN:
        return "RETURN";
      case Opcodes.ATHROW:
        return "ATHROW";
    }
    return "unknonw (" + opcode + ")";
  }
}

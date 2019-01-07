package jp.skypencil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

public class Main {
  public Main() {
    if (System.currentTimeMillis() % 2 == 0) {
      System.out.println("0");
    } else {
      System.out.println("1");
    }
  }

  public static void main(String... args) throws IOException, AnalyzerException {
    try (InputStream input =
        Files.newInputStream(Paths.get("build/classes/java/main", "jp/skypencil/Main.class"))) {
      ClassReader classReader = new ClassReader(input);
      ClassNode classNode = new ClassNode();
      classReader.accept(classNode, ClassReader.SKIP_DEBUG);

      System.out.printf("digraph %s {%n", classNode.name.replace('/', '_'));
      int methodIndex = 0;
      for (MethodNode method : classNode.methods) {
        if (method.instructions.size() > 0) {
          System.out.printf("  subgraph cluster_method_%d {%n    label=\"#%s\";%n    graph[style=dotted];%n", methodIndex, method.name, method.name);
          final int index = methodIndex;
          for (int i = 0; i < method.instructions.size(); ++i) {
              AbstractInsnNode insn = method.instructions.get(i);
              System.out.printf("    edge%d_%d [label=\"%s\"];%n", index, i, insn.getClass());
          }

          Analyzer<BasicValue> analyzer =
              new Analyzer<BasicValue>(new BasicInterpreter()) {
                @Override
                protected void newControlFlowEdge(final int insnIndex, final int successorIndex) {
                  System.out.printf("    edge%d_%d -> edge%d_%d;%n", index, insnIndex, index, successorIndex);
                }

                @Override
                protected boolean newControlFlowExceptionEdge(final int insnIndex, final int successorIndex) {
                  System.out.printf("    edge%d_%d -> edge%d_%d [color = \"red\"];%n", index, insnIndex, index, successorIndex);
                  return true;
                }
              };
          analyzer.analyze(classNode.name, method);
          System.out.println("  }");
        }
        methodIndex++;
      }
      System.out.println("}");
    }
  }
}

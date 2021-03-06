package jp.skypencil;

import java.util.List;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;

public class CloseableInterpreter extends Interpreter<CloseableValue> {
  private static final CloseableValue UNINITIALIZED_VALUE = new CloseableValue(true);

  public CloseableInterpreter() {
    this(Opcodes.ASM7);
  }

  public CloseableInterpreter(int api) {
    super(api);
  }

  @Override
  public CloseableValue newValue(Type type) {
    if (type == null) {
      return UNINITIALIZED_VALUE;
    } else if (type.getSort() == Type.VOID) {
      return null;
    } else if (type.getSort() != Type.OBJECT) {
      return new CloseableValue(true);
    }
    // TODO confirm that object implements AutoCloseable
    return new CloseableValue(false);
  }

  @Override
  public CloseableValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
    final Type type;
    switch (insn.getOpcode()) {
      case Opcodes.GETSTATIC:
        // assume that we do not store closed instance to field
        type = Type.getType(((FieldInsnNode) insn).desc);
        break;
      case Opcodes.NEW:
        type = Type.getObjectType(((TypeInsnNode) insn).desc);
        break;
      default:
        type = null;
    }
    return newValue(type);
  }

  @Override
  public CloseableValue copyOperation(AbstractInsnNode insn, CloseableValue value)
      throws AnalyzerException {
    return value;
  }

  @Override
  public CloseableValue unaryOperation(AbstractInsnNode insn, CloseableValue value)
      throws AnalyzerException {
    // the CloseableValue is stateless, so we can reuse the given reference
    return value;
  }

  @Override
  public CloseableValue binaryOperation(
      AbstractInsnNode insn, CloseableValue value1, CloseableValue value2)
      throws AnalyzerException {
    final Type type;
    switch (insn.getOpcode()) {
      case Opcodes.GETFIELD:
        // assume that we do not store closed instance to field
        type = Type.getType(((FieldInsnNode) insn).desc);
        break;
      case Opcodes.CHECKCAST:
        type = Type.getObjectType(((TypeInsnNode) insn).desc);
        break;
      default:
        type = null;
    }
    return newValue(type);
  }

  @Override
  public CloseableValue ternaryOperation(
      AbstractInsnNode insn, CloseableValue value1, CloseableValue value2, CloseableValue value3)
      throws AnalyzerException {
    return null;
  }

  @Override
  public CloseableValue naryOperation(AbstractInsnNode insn, List<? extends CloseableValue> values)
      throws AnalyzerException {
    final Type type;
    switch (insn.getOpcode()) {
      case Opcodes.MULTIANEWARRAY:
        type = null;
        break;
      case Opcodes.INVOKEVIRTUAL:
      case Opcodes.INVOKESPECIAL:
      case Opcodes.INVOKESTATIC:
      case Opcodes.INVOKEINTERFACE:
        MethodInsnNode methodInsn = (MethodInsnNode) insn;
        if ("close".equals(methodInsn.name) && "()V".equals(methodInsn.desc)) {
          // TODO confirm that the target instance implements AutoCloseable
          System.err.println("Wow closed!");
          return new CloseableValue(true);
        } else {
          type = Type.getReturnType(methodInsn.desc);
        }
        break;
      case Opcodes.INVOKEDYNAMIC:
        InvokeDynamicInsnNode dynamicInsn = (InvokeDynamicInsnNode) insn;
        // TODO need to find a Handler from bsmArgs?
        Handle handle = (Handle) dynamicInsn.bsmArgs[1];
        if ("close".equals(handle.getName()) && "()V".equals(handle.getDesc())) {
          // TODO confirm that the target instance implements AutoCloseable
          System.err.println("Wow closed!");
          return new CloseableValue(true);
        } else {
          type = Type.getReturnType(dynamicInsn.desc);
        }
        break;
      default:
        type = null;
    }
    return newValue(type);
  }

  @Override
  public void returnOperation(AbstractInsnNode insn, CloseableValue value, CloseableValue expected)
      throws AnalyzerException {}

  @Override
  public CloseableValue merge(CloseableValue value1, CloseableValue value2) {
    return new CloseableValue(value1.isClosed() && value2.isClosed());
  }
}

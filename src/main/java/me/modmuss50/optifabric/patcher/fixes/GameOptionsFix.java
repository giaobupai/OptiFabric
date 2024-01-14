package me.modmuss50.optifabric.patcher.fixes;

import com.google.common.collect.MoreCollectors;
import me.modmuss50.optifabric.util.RemappingUtils;
import org.apache.commons.lang3.Validate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class GameOptionsFix implements ClassFixer {

    private final String gameOptionsName = RemappingUtils.getClassName("class_315");
    private final String loadName = RemappingUtils.getMethodName("class_315", "method_1636", "()V");

    @Override
    public void fix(ClassNode optifine, ClassNode minecraft) {
        optifine.methods.removeIf(method -> loadName.equals(method.name));
        optifine.methods.removeIf(method -> "load".equals(method.name));
        //lambda$load(NbtCompound,String)void
        optifine.methods.removeIf(method -> (method.access | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC) == method.access
                        && method.name.startsWith("lambda$load$")
                        && method.desc.equals(RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_2487;Ljava/lang/String;)V")));

        MethodNode method = minecraft.methods.stream().filter(node -> loadName.equals(node.name)).collect(MoreCollectors.onlyElement());
        Validate.notNull(method, "old method null");

        MethodNode lambda = minecraft.methods.stream().filter(node -> "method_24230".equals(node.name)).collect(MoreCollectors.onlyElement());
        Validate.notNull(method, "old method lambda null");

        //re-add the Optifine stuff
        InsnList head = new InsnList();
        head.add(new VarInsnNode(Opcodes.ALOAD, 0));
        head.add(new InsnNode(Opcodes.ICONST_1));
        head.add(new FieldInsnNode(Opcodes.PUTFIELD, gameOptionsName, "loadOptions", "Z"));

        InsnList tail = new InsnList();
        tail.add(new VarInsnNode(Opcodes.ALOAD, 0));
        tail.add(new InsnNode(Opcodes.ICONST_0));
        tail.add(new FieldInsnNode(Opcodes.PUTFIELD, gameOptionsName, "loadOptions", "Z"));
        tail.add(new VarInsnNode(Opcodes.ALOAD, 0));
        tail.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, gameOptionsName, "loadOfOptions", "()V", false));

        method.instructions.insertBefore(method.instructions.getFirst(), head);
        method.instructions.insertBefore(method.instructions.getLast().getPrevious(), tail);

        optifine.methods.add(method);
        //failsafe for 1.18.2
        if (optifine.methods.stream().noneMatch(node -> node.name.equals(lambda.name) && node.desc.equals(lambda.desc))) {
            optifine.methods.add(lambda);
        }
    }
}

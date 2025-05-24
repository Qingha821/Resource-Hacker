package studio.sp.mixin;

import studio.sp.MrpClient;
import studio.sp.drm.ForgeAccessHack;

import net.minecraft.server.packs.FilePackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

@Mixin(FilePackResources.class)
public class FilePackResourceMixin {
    @ModifyVariable(
            method = "<init>",
            at = @At(value = "HEAD", ordinal = 0),
            argsOnly = true
    )
    private static File injectConstructor(File file) {
        try {
            byte[] original = Files.readAllBytes(file.toPath());

            String key = MrpClient.getKey();
            if (key == null) {
                // Not configured yet
                MrpClient.LOG.info("Resource not decrypted (Not configured yet)");
                return file;
            }
            if (key.equals("NULL_KEY")) {
                // NULL_KEY (default configuration)
                MrpClient.LOG.warn("Null key received from server");
                return file;
            }
            if (ForgeAccessHack.accessValidate(original)) {
                String yee = UUID.randomUUID().toString();
                File tmp = File.createTempFile(yee.substring(0, 8), null);

                byte[] dec = ForgeAccessHack.accessDecrypt(key, original);
                Files.write(tmp.toPath(), dec);

                MrpClient.LOG.info("Resource decryption completed");
                return tmp;
            } else {
                // Not encrypted
                return file;
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}

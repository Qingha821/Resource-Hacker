package studio.sp.mixin;

import studio.sp.MrpClient;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Inject(
            method = "disconnect",
            at = @At("TAIL")
    )
    public void injectDisconnect(Component component, CallbackInfo ci) {
        MrpClient.getLoadedUrls().clear();
    }
}

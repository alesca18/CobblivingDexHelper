package org.main.cobblivingdexhelper.mixins;

import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.main.cobblivingdexhelper.network.SortPCPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(Screen.class)
public abstract class PCGUIMixin {

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

    // Aggiornato Path Texture
    @Unique
    private static final ResourceLocation SORT_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblivingdexhelper", "textures/pc/sort_button.png");

    @Inject(method = "init", at = @At("TAIL"))
    private void addSorterButton(Minecraft client, int width, int height, CallbackInfo ci) {
        if ((Object) this instanceof PCGUI) {
            int buttonX = (width / 2) + 60;
            int buttonY = (height / 2) - 115;

            Button customButton = new Button(buttonX, buttonY, 21, 18, Component.empty(), button -> {
                this.onSortClicked();
            }, Supplier::get) {
                @Override
                public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                    graphics.pose().pushPose();
                    graphics.blit(SORT_BUTTON_TEXTURE, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), 21, 18);
                    graphics.pose().popPose();
                }
            };
            this.addRenderableWidget(customButton);
        }
    }

    @Unique
    private void onSortClicked() {
        if (ClientPlayNetworking.canSend(SortPCPayload.ID)) {
            ClientPlayNetworking.send(new SortPCPayload());
        }
    }
}
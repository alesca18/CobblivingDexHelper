package org.main.cobblivingdexhelper.mixins;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.pokedex.PokedexScannerRenderer;
import com.cobblemon.mod.common.client.storage.ClientBox;
import com.cobblemon.mod.common.client.storage.ClientPC;
import com.cobblemon.mod.common.pokedex.scanner.PokedexUsageContext;
import com.cobblemon.mod.common.pokedex.scanner.ScannableEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PokedexScannerRenderer.class)
public class PokedexScannerRendererMixin {

    // Aggiornato Path Texture
    @Unique
    private static final ResourceLocation PC_ICON = ResourceLocation.fromNamespaceAndPath("cobblivingdexhelper", "textures/pc/pc_icon.png");
    @Unique
    private static final ResourceLocation PC_ICON_EVO = ResourceLocation.fromNamespaceAndPath("cobblivingdexhelper", "textures/pc/pc_icon_evo.png");

    @Inject(method = "renderScanOverlay", at = @At("TAIL"))
    public void renderPCIcon(GuiGraphics graphics, float tickDelta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        PokedexUsageContext usageContext = CobblemonClient.INSTANCE.getPokedexUsageContext();
        if (usageContext == null || usageContext.getFocusIntervals() <= 0) return;

        ScannableEntity result = usageContext.getScannableEntityInFocus();
        if (result != null) {
            var scanResult = result.resolvePokemonScan();
            if (scanResult == null) return;

            Pokemon pkm = scanResult.getPokemon();
            if (pkm == null) return;

            ClientPC summaryPC = CobblemonClient.INSTANCE.getStorage().getPcStores().get(client.player.getUUID());
            if (summaryPC == null) return;

            boolean hasSpecies = hasSpeciesInPC(pkm.getSpecies().getName(), summaryPC);

            if (hasSpecies) {
                boolean missingEvo = hasMissingEvolutionInPC(pkm, summaryPC);
                if (missingEvo) {
                    renderIconAttached(graphics, client, pkm, usageContext, PC_ICON_EVO);
                } else {
                    renderIconAttached(graphics, client, pkm, usageContext, PC_ICON);
                }
            }
        }
    }

    // ... (Il resto dei metodi helper come hasMissingEvolutionInPC, renderIconAttached, hasSpeciesInPC rimangono identici al codice precedente, assicurati di includerli)

    @Unique
    private boolean hasMissingEvolutionInPC(Pokemon pkm, ClientPC summaryPC) {
        return checkRecursiveEvolutions(pkm.getSpecies(), summaryPC);
    }

    @Unique
    private boolean checkRecursiveEvolutions(Species currentSpecies, ClientPC summaryPC) {
        var evolutions = currentSpecies.getEvolutions();
        if (evolutions.isEmpty()) return false;
        for (var evo : evolutions) {
            String evoName = evo.getResult().getSpecies();
            if (!hasSpeciesInPC(evoName, summaryPC)) return true;
            Pokemon nextStagePkm = new Pokemon();
            evo.getResult().apply(nextStagePkm);
            if (checkRecursiveEvolutions(nextStagePkm.getSpecies(), summaryPC)) return true;
        }
        return false;
    }

    @Unique
    private void renderIconAttached(GuiGraphics graphics, Minecraft client, Pokemon pkm, PokedexUsageContext usageContext, ResourceLocation icon) {
        List<Boolean> availableFrames = usageContext.getAvailableInfoFrames();
        if (availableFrames == null || availableFrames.isEmpty()) return;

        int infoDisplayedCounter = 0;
        for (int index = 0; index < availableFrames.size(); index++) {
            Boolean isLeftSide = availableFrames.get(index);
            if (isLeftSide == null) continue;
            if (infoDisplayedCounter > 1) return;
            infoDisplayedCounter++;

            if (infoDisplayedCounter == 2) {
                boolean isInnerFrame = (index == 1 || index == 2);
                int innerStemWidth = 28;
                int innerFrameWidth = 120;
                int xOffset = (isInnerFrame ? -177 : -120) + (isLeftSide ? 0 : (isInnerFrame ? 234 : 148));
                int centerX = client.getWindow().getGuiScaledWidth() / 2;
                int centerY = client.getWindow().getGuiScaledHeight() / 2;

                int yOffset = 0;
                if (index == 0) yOffset = -80;
                else if (index == 1) yOffset = -26;
                else if (index == 2) yOffset = 6;
                else if (index == 3) yOffset = 25;

                int xOffsetText = isInnerFrame ? ((innerFrameWidth - innerStemWidth) / 2) + (isLeftSide ? 0 : innerStemWidth) : 46;
                int yOffsetText = (index == 3) ? 42 : (index == 2 ? 8 : (index == 0 ? 5 : 4));
                int yOffsetName = (pkm.getOwnerUUID() != null) ? 2 : 0;

                int textCenterX = centerX + xOffset + xOffsetText;
                int textCenterY = centerY + yOffset + yOffsetText + yOffsetName;

                int textWidth = client.font.width(pkm.getSpecies().getName());
                int iconSize = 8;
                int iconX = textCenterX + (textWidth / 2) + 4;
                int iconY = textCenterY + 2 - (iconSize / 2);

                graphics.blit(icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
                return;
            }
        }
    }

    @Unique
    private boolean hasSpeciesInPC(String speciesName, ClientPC summaryPC) {
        for (ClientBox box : summaryPC.getBoxes()) {
            for (Pokemon pkm : box.getSlots()) {
                if (pkm != null && pkm.getSpecies().getName().equalsIgnoreCase(speciesName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
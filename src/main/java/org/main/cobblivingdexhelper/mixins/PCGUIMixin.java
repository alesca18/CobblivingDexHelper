package org.main.cobblivingdexhelper.mixins;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.storage.ClientBox;
import com.cobblemon.mod.common.client.storage.ClientPC;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(PCGUI.class)
public abstract class PCGUIMixin {

    @Shadow private ClientPC pc;

    @Unique
    private boolean isReadOnlyMode = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(Component title, int boxNumber, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        if (isHoldingPokedex(player)) {
            this.isReadOnlyMode = true;
            this.pc = createSortedVirtualPC(player.getUUID());
        }
    }

    @Unique
    private boolean isHoldingPokedex(Player player) {
        ResourceLocation pokedexId = ResourceLocation.fromNamespaceAndPath("cobblemon", "pokedex");
        return player.getMainHandItem().is(BuiltInRegistries.ITEM.get(pokedexId)) ||
                player.getOffhandItem().is(BuiltInRegistries.ITEM.get(pokedexId));
    }

    @Unique
    private ClientPC createSortedVirtualPC(UUID playerUUID) {
        ClientPC realPC = CobblemonClient.INSTANCE.getStorage().getPcStores().get(playerUUID);

        // Se il PC reale è null, ne creiamo uno vuoto (0 è l'indice del box selezionato)
        if (realPC == null) return new ClientPC(playerUUID, 0);

        List<Pokemon> allPokemon = new ArrayList<>();
        for (ClientBox box : realPC.getBoxes()) {
            for (Pokemon p : box.getSlots()) {
                if (p != null) {
                    allPokemon.add(p);
                }
            }
        }

        ClientPC virtualPC = new ClientPC(playerUUID, 0);
        ResourceLocation defaultTexture = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pc/box_background.png");

        // --- INIZIO REFLECTION ---
        Constructor<?> boxConstructor = null;
        try {
            // Cerchiamo il costruttore: ClientBox(MutableComponent, ResourceLocation)
            boxConstructor = ClientBox.class.getConstructor(MutableComponent.class, ResourceLocation.class);
        } catch (NoSuchMethodException e) {
            // Se fallisce, proviamo a stampare l'errore ma non crashiamo subito (fallback o rethrow)
            throw new RuntimeException("CobblivingDexHelper: Impossibile trovare il costruttore di ClientBox via Reflection. Errore di Mappings?", e);
        }
        // --- FINE REFLECTION ---

        for (int i = 0; i < 40; i++) {
            int start = (i * 30) + 1;
            int end = (i + 1) * 30;
            MutableComponent boxName = Component.literal(start + "-" + end);

            ClientBox box;
            try {
                // Istanziamo il box usando il costruttore trovato via reflection
                box = (ClientBox) boxConstructor.newInstance(boxName, defaultTexture);
            } catch (Exception e) {
                throw new RuntimeException("CobblivingDexHelper: Errore durante l'istanziazione di ClientBox.", e);
            }

            // Riempiamo gli slot vuoti per sicurezza
            while (box.getSlots().size() < 30) {
                box.getSlots().add(null);
            }

            virtualPC.getBoxes().add(box);
        }

        // Logica di ordinamento (resta invariata)
        for (Pokemon p : allPokemon) {
            int dexNum = p.getSpecies().getNationalPokedexNumber();
            if (dexNum > 0) {
                int index = dexNum - 1;
                int boxIndex = index / 30;
                int slotIndex = index % 30;

                if (boxIndex < virtualPC.getBoxes().size()) {
                    ClientBox targetBox = virtualPC.getBoxes().get(boxIndex);
                    targetBox.getSlots().set(slotIndex, p);
                }
            }
        }

        return virtualPC;
    }
}
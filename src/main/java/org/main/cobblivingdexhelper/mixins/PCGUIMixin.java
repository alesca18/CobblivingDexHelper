package org.main.cobblivingdexhelper.mixins;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.pc.PCGUI;
import com.cobblemon.mod.common.client.gui.pc.PCGUIConfiguration;
import com.cobblemon.mod.common.client.storage.ClientBox;
import com.cobblemon.mod.common.client.storage.ClientPC;
import com.cobblemon.mod.common.client.storage.ClientParty;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

@Mixin(PCGUI.class)
public abstract class PCGUIMixin {

    @Unique
    private boolean isReadOnlyMode = false;

    // AGGIORNATO: La firma dell'iniezione ora corrisponde esattamente a quella richiesta dal crash report
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(ClientPC pc, ClientParty party, PCGUIConfiguration config, int boxNumber, Set<UUID> selected, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        if (isHoldingPokedex(player)) {
            this.isReadOnlyMode = true;
            ClientPC virtualPC = createSortedVirtualPC(player.getUUID());

            // Impostiamo il PC tramite Reflection sicura
            setPCFieldViaReflection(virtualPC);
        }
    }

    @Unique
    private void setPCFieldViaReflection(ClientPC newPC) {
        try {
            // Cerca un campo di tipo ClientPC dentro la classe PCGUI
            Object guiInstance = (Object) this;

            for (Field f : guiInstance.getClass().getDeclaredFields()) {
                if (f.getType() == ClientPC.class) {
                    f.setAccessible(true);
                    f.set(guiInstance, newPC);
                    return;
                }
            }
            System.err.println("CobblivingDexHelper: Impossibile trovare il campo ClientPC in PCGUI via Reflection.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Unique
    private boolean isPokedex(net.minecraft.world.item.ItemStack stack) {
        // Se lo stack è vuoto, non è un pokedex (risolve il problema del "sempre true")
        if (stack.isEmpty()) return false;

        // Otteniamo l'ID dell'oggetto in mano
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());

        // Controlliamo se è di Cobblemon E se nel nome c'è "pokedex"
        // Questo funziona con "cobblemon:pokedex", "cobblemon:red_pokedex", ecc.
        return id.getNamespace().equals("cobblemon") && id.getPath().contains("pokedex");
    }
    @Unique
    private boolean isHoldingPokedex(Player player) {
        return isPokedex(player.getMainHandItem()) || isPokedex(player.getOffhandItem());
    }

    @Unique
    private ClientPC createSortedVirtualPC(UUID playerUUID) {
        ClientPC realPC = CobblemonClient.INSTANCE.getStorage().getPcStores().get(playerUUID);

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

        // --- REFLECTION ---
        Constructor<?> boxConstructor = null;
        try {
            // Cerchiamo un costruttore che accetta 2 parametri (Component, ResourceLocation)
            boxConstructor = Arrays.stream(ClientBox.class.getConstructors())
                    .filter(c -> c.getParameterCount() == 2)
                    .findFirst()
                    .orElseThrow(() -> new NoSuchMethodException("Nessun costruttore a 2 argomenti trovato in ClientBox"));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("CobblivingDexHelper: Errore critico Reflection ClientBox.", e);
        }
        // ------------------

        for (int i = 0; i < 40; i++) {
            int start = (i * 30) + 1;
            int end = (i + 1) * 30;
            MutableComponent boxName = Component.literal(start + "-" + end);

            ClientBox box;
            try {
                box = (ClientBox) boxConstructor.newInstance(boxName, defaultTexture);
            } catch (Exception e) {
                throw new RuntimeException("CobblivingDexHelper: Errore durante l'istanziazione di ClientBox.", e);
            }

            while (box.getSlots().size() < 30) {
                box.getSlots().add(null);
            }

            virtualPC.getBoxes().add(box);
        }

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
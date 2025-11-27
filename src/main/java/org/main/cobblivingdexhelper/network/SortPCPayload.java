package org.main.cobblivingdexhelper.network;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.pc.PCPosition;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.storage.ClientPC;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.*;

public record SortPCPayload() implements CustomPacketPayload {

    // Aggiornato Namespace: cobblemonsorter -> cobblivingdexhelper
    public static final Type<SortPCPayload> ID = new Type<>(
            ResourceLocation.fromNamespaceAndPath("cobblivingdexhelper", "sort_pc")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SortPCPayload> CODEC = StreamCodec.unit(new SortPCPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    // --- LOGICA LATO SERVER ---
    public static void handleServerSide(ServerPlayer player) {
        if (player == null) return;
        PCStore serverPC;
        try {
            // REFLECTION FIX
            Object storageManager = Cobblemon.INSTANCE.getStorage();
            Method getPCMethod = Arrays.stream(storageManager.getClass().getMethods())
                    .filter(m -> m.getName().equals("getPC")
                            && m.getParameterCount() == 2
                            && m.getParameterTypes()[0].equals(UUID.class))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Metodo getPC non trovato!"));

            serverPC = (PCStore) getPCMethod.invoke(storageManager, player.getUUID(), player.registryAccess());

        } catch (Exception e) {
            System.err.println("Errore critico CobblivingDexHelper: Impossibile recuperare il PC.");
            e.printStackTrace();
            return;
        }
        System.out.println("Server: Ordinamento PC richiesto da " + player.getName().getString());

        ClientPC clientPC = CobblemonClient.INSTANCE.getStorage().getPcStores().get(player.getUUID());
        // --- LOGICA ORDINAMENTO ---
        List<Pokemon> allPokemon = new ArrayList<>();
        // Usa 40 box come richiesto
        for (int box = 0; box < 40; box++) {

            // Check box esistenti e nomi "box"
            if(serverPC.getBoxes().size() > box && serverPC.getBoxes().get(box) != null) {
                String boxName = serverPC.getBoxes().get(box).getName();
                if (boxName != null && boxName.toLowerCase().contains("box")) {
                    continue; // Salta i box protetti
                }
            }

            for (int slot = 0; slot < 30; slot++) {
                PCPosition currentPos = new PCPosition(box, slot);
                Pokemon pkm = serverPC.get(currentPos);

                if (pkm != null) {
                    allPokemon.add(pkm);
                   // serverPC.remove(pkm);
                    clientPC.remove(pkm.getUuid());
                }
            }
        }

        // Separazione
        Set<String> seenSpecies = new HashSet<>();
        List<Pokemon> uniques = new ArrayList<>();
        List<Pokemon> duplicates = new ArrayList<>();

        for (Pokemon pkm : allPokemon) {
            String species = pkm.getSpecies().getName();
            if (!seenSpecies.add(species)) {
                duplicates.add(pkm);
            } else {
                uniques.add(pkm);
            }
        }

        // Piazza Unici
        for (Pokemon pkm : uniques) {
            Species species = pkm.getSpecies();
            int index = species.getNationalPokedexNumber();
            if (index >= 0) {
                int adjustedIndex = index - 1;
                if (adjustedIndex < 0) adjustedIndex = 0;

                int box = adjustedIndex / 30;
                int slot = adjustedIndex % 30;

                if (box < 40) {
                    PCPosition targetPos = new PCPosition(box, slot);
                    //Pokemon existing = serverPC.get(targetPos);
                    Pokemon existing = clientPC.get(targetPos);
                    if (existing != null) {
                        duplicates.add(existing);
                    }
                   // serverPC.set(targetPos, pkm);
                    clientPC.set(targetPos,pkm);
                } else {
                    duplicates.add(pkm);
                }
            } else {
                duplicates.add(pkm);
            }
        }

        // Piazza Duplicati
        /*
        int box = serverPC.getBoxes().size() - 1;
        int slot = 0;

        for (Pokemon pkm : duplicates) {
            boolean placed = false;
            while (!placed && box >= 0) {
                PCPosition pos = new PCPosition(box, slot);
                if (serverPC.get(pos) == null) {
                    serverPC.set(pos, pkm);
                    placed = true;
                } else {
                    slot++;
                    if (slot >= 30) {
                        slot = 0;
                        box--;
                    }
                }
            }
            if (!placed) {
                System.err.println("PC Full! Could not place " + pkm.getSpecies().getName());
            }


        }

         */
    }
}
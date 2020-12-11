package net.teamfruit.improvednoteblock;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class ImprovedNoteBlock extends JavaPlugin implements Listener {

    private NamespacedKey blockKey = new NamespacedKey(this, "improved_noteblock");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBlock(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.NOTE_BLOCK)
            return;

        ItemStack itemStack = event.getItemInHand();
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(blockKey, PersistentDataType.BYTE))
            return;

        Byte id = container.get(blockKey, PersistentDataType.BYTE);
        if (id == null)
            return;

        Block block = event.getBlockPlaced();
        BlockData data = block.getBlockData();
        if (!(data instanceof NoteBlock))
            return;
        NoteBlock noteblock = (NoteBlock) data;

        Note note = new Note(id);
        noteblock.setNote(note);
        block.setBlockData(noteblock);

        Player player = event.getPlayer();

        String detail = getInfo(note);
        player.sendActionBar(detail);
        playNote(block, noteblock.getInstrument(), note);
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.NOTE_BLOCK)
            return;

        BlockData data = block.getBlockData();
        if (!(data instanceof NoteBlock))
            return;
        NoteBlock noteblock = (NoteBlock) data;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.END_ROD)
            return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Action action = event.getAction();

        Note noteEdit = noteblock.getNote();
        if (action == Action.RIGHT_CLICK_BLOCK) {
            if (player.isSneaking())
                noteblock.setNote(noteEdit = new Note((noteEdit.getId() + 24) % 25));
            else
                noteblock.setNote(noteEdit = new Note((noteEdit.getId() + 1) % 25));
            block.setBlockData(noteblock);
        }
        Note note = noteEdit;

        String detail = getInfo(note);

        if (action == Action.LEFT_CLICK_BLOCK) {
            if (player.isSneaking()) {
                boolean bCreative = player.getGameMode() == GameMode.CREATIVE;
                PlayerInventory inventory = player.getInventory();
                ItemStack noteBlock = new ItemStack(Material.NOTE_BLOCK);

                ItemStack itemStack = new ItemStack(Material.NOTE_BLOCK);
                ItemMeta meta = itemStack.getItemMeta();
                meta.getPersistentDataContainer().set(blockKey, PersistentDataType.BYTE, note.getId());
                meta.setLore(Arrays.asList(
                        ChatColor.YELLOW + "音階が設定されたのブロック",
                        ChatColor.GRAY + "音符ブロックにエンドロッドを",
                        ChatColor.GRAY + "Shift左クリックでコピーを作成できる"
                ));
                meta.setDisplayName(detail);
                itemStack.setItemMeta(meta);

                boolean bSufficient = bCreative;
                if (!bSufficient) {
                    Optional<ItemStack> opt = Arrays.stream(inventory.getContents())
                            .filter(Objects::nonNull)
                            .filter(noteBlock::isSimilar)
                            .findFirst();
                    opt.ifPresent(e -> e.setAmount(e.getAmount() - 1));
                    bSufficient = opt.map(e -> e.getAmount() > 0).orElse(false);
                }
                if (bSufficient)
                    player.getWorld().dropItem(player.getLocation(), itemStack);
            }
        }

        player.sendActionBar(detail);
        playNote(block, noteblock.getInstrument(), note);
    }

    private void playNote(Block block, Instrument instrument, Note note) {
        Location loc = block.getLocation();
        loc.getNearbyPlayers(16.0 * 3).forEach(p -> p.playNote(loc.clone().add(0, 1, 0), instrument, note));
        loc.getWorld().spawnParticle(Particle.NOTE, loc.clone().add(.5, 1.2, .5), 1, 0.0, 0.0, 0.0, note.getId() / 24.0);
    }

    private String getInfo(Note note) {
        String[] noteJp = new String[]{"ソ", "ラ", "シ", "ド", "レ", "ミ", "ファ"};
        ChatColor[] noteOct = new ChatColor[]{ChatColor.GRAY, ChatColor.WHITE, ChatColor.YELLOW};
        return String.format("%s%d+%d %s%s (%s%3$s)", noteOct[note.getOctave()], note.getOctave(), note.getId(), note.getTone(), note.isSharped() ? "♯" : " ", noteJp[note.getTone().ordinal()]);
    }

}

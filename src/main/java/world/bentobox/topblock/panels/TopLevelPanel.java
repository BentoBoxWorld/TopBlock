///
// Created by BONNe
// Copyright - 2021
///

package world.bentobox.topblock.panels;


import java.io.File;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.topblock.TopBlock;
import world.bentobox.topblock.TopBlockManager.TopTenData;
import world.bentobox.topblock.util.Utils;


/**
 * This panel opens the top ten panel for AOneBlock.
 */
public class TopLevelPanel {

    private static final String REFERENCE = "topblock.gui.buttons.island.";
    private static final String PLAYER = "[player]";

    private final TopBlock addon;
    private final User user;
    private final World world;
    private final String iconPermission;
    private final List<TopTenData> topIslands;


    private TopLevelPanel(TopBlock addon, User user, World world, String permissionPrefix) {
        this.addon = addon;
        this.user = user;
        this.world = world;
        this.iconPermission = permissionPrefix + "topblock.icon";
        this.topIslands = this.addon.getManager().getTopTen(TopBlock.TEN);
    }


    /**
     * Open the panel for a user.
     */
    public static void openPanel(TopBlock addon, User user, World world, String permissionPrefix) {
        new TopLevelPanel(addon, user, world, permissionPrefix).build();
    }


    private void build() {
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();
        panelBuilder.user(this.user);
        panelBuilder.world(this.world);
        panelBuilder.template("top_panel", new File(this.addon.getDataFolder(), "panels"));
        panelBuilder.registerTypeBuilder("TOP", this::createPlayerButton);
        panelBuilder.build();
    }


    private PanelItem createPlayerButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot itemSlot) {
        int index = (int) template.dataMap().getOrDefault("index", 0);

        if (index < 1) {
            return this.createFallback(template.fallback(), index);
        }

        TopTenData record = this.topIslands.size() < index ? null : this.topIslands.get(index - 1);

        if (record == null) {
            return this.createFallback(template.fallback(), index);
        }

        return this.createIslandIcon(template, record, index);
    }


    private PanelItem createFallback(ItemTemplateRecord template, long index) {
        if (template == null) {
            return null;
        }

        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null) {
            builder.icon(template.icon().clone());
        }

        if (template.title() != null) {
            builder.name(this.user.getTranslation(this.world, template.title(),
                    TextVariables.NAME, String.valueOf(index)));
        } else {
            builder.name(this.user.getTranslation(this.world, REFERENCE,
                    TextVariables.NAME, String.valueOf(index)));
        }

        if (template.description() != null) {
            builder.description(this.user.getTranslation(this.world, template.description(),
                    TextVariables.NUMBER, String.valueOf(index)));
        }

        builder.amount(index != 0 ? (int) index : 1);

        return builder.build();
    }


    private PanelItem createIslandIcon(ItemTemplateRecord template, TopTenData record, int index) {
        Island island = record.island();

        if (island == null) {
            return this.createFallback(template.fallback(), index);
        }

        PanelItemBuilder builder = new PanelItemBuilder();

        this.populateIslandIcon(builder, template, island);
        this.populateIslandTitle(builder, template, island);
        this.populateIslandDescription(builder, template, island, record, index);

        builder.amount(index);

        return builder.build();
    }


    private void populateIslandTitle(PanelItemBuilder builder, ItemTemplateRecord template, Island island) {
        String nameText;

        if (island.getName() == null || island.getName().isEmpty()) {
            nameText = this.user.getTranslation(REFERENCE + "owners-island", PLAYER,
                    island.getOwner() == null
                            ? this.user.getTranslation(REFERENCE + "unknown")
                            : this.addon.getPlayers().getName(island.getOwner()));
        } else {
            nameText = island.getName();
        }

        if (template.title() != null && !template.title().isBlank()) {
            builder.name(this.user.getTranslation(this.world, template.title(),
                    TextVariables.NAME, nameText));
        } else {
            builder.name(this.user.getTranslation(REFERENCE + "name", TextVariables.NAME, nameText));
        }
    }


    private void populateIslandIcon(PanelItemBuilder builder, ItemTemplateRecord template, Island island) {
        User owner = island.getOwner() == null ? null : User.getInstance(island.getOwner());

        String permissionIcon = owner == null ? null
                : Utils.getPermissionValue(owner, this.iconPermission, null);

        Material material = (permissionIcon != null && !permissionIcon.equals("*"))
                ? Material.matchMaterial(permissionIcon)
                : null;

        if (material != null) {
            if (!material.equals(Material.PLAYER_HEAD)) {
                builder.icon(material);
            } else {
                builder.icon(owner.getName());
            }
        } else if (template.icon() != null) {
            builder.icon(template.icon().clone());
        } else if (owner != null) {
            builder.icon(owner.getName());
        } else {
            builder.icon(Material.PLAYER_HEAD);
        }
    }


    private void populateIslandDescription(PanelItemBuilder builder, ItemTemplateRecord template,
            Island island, TopTenData record, int index) {

        String ownerText = this.user.getTranslation(REFERENCE + "owner", PLAYER,
                island.getOwner() == null
                        ? this.user.getTranslation(REFERENCE + "unknown")
                        : this.addon.getPlayers().getName(island.getOwner()));

        String memberText;
        if (island.getMemberSet().size() > 1) {
            StringBuilder memberBuilder = new StringBuilder(
                    this.user.getTranslationOrNothing(REFERENCE + "members-title"));
            for (UUID uuid : island.getMemberSet()) {
                User u = User.getInstance(uuid);
                if (memberBuilder.length() > 0) {
                    memberBuilder.append("\n");
                }
                memberBuilder.append(this.user.getTranslationOrNothing(REFERENCE + "member",
                        PLAYER, u.getName()));
            }
            memberText = memberBuilder.toString();
        } else {
            memberText = "";
        }

        String placeText = this.user.getTranslation(REFERENCE + "place",
                TextVariables.NUMBER, String.valueOf(index));

        String countText = this.user.getTranslation(REFERENCE + "count",
                TextVariables.NUMBER, this.addon.getManager().formatLevel((long) record.blockNumber()));

        String lifetimeText = this.user.getTranslation(REFERENCE + "lifetime",
                TextVariables.NUMBER, this.addon.getManager().formatLevel(record.lifetime()));

        String descriptionTemplate = template.description() != null && !template.description().isBlank()
                ? template.description()
                : REFERENCE + "description";

        String descriptionText = this.user.getTranslation(this.world, descriptionTemplate,
                "[owner]", ownerText,
                "[members]", memberText,
                "[count]", countText,
                "[lifetime]", lifetimeText,
                "[place]", placeText);

        builder.description(descriptionText
                .replaceAll("(?m)^[ \\t]*\\r?\\n", "")
                .replaceAll("(?<!\\\\)\\|", "\n")
                .replace("\\\\\\|", "|"));
    }
}

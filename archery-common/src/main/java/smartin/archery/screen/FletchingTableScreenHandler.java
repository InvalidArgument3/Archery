package smartin.archery.screen;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import smartin.archery.Archery;
import smartin.miapi.Miapi;
import smartin.miapi.item.modular.ModularItem;
import smartin.miapi.modules.ItemModule;
import smartin.miapi.registries.RegistryInventory;

public class FletchingTableScreenHandler extends AbstractContainerMenu {
    public static final int ARROW_INPUT_SLOT = 0;
    public static final int SHAFT_SLOT = 1;
    public static final int HEAD_SLOT = 2;
    public static final int TAIL_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;

    public static final int INPUT_SLOTS = 4;
    public static final int CONTAINER_SIZE = 5;


    private final Player player;
    private final Container container;

    public ItemModule shaftModule =
            RegistryInventory.ITEM_MODULE_MIAPI_REGISTRY.get(Miapi.id("tm_archery:arrow/shaft/normal"));
    public ItemModule headModule =
            RegistryInventory.ITEM_MODULE_MIAPI_REGISTRY.get(Miapi.id("tm_archery:arrow/head/normal"));
    public ItemModule tailModule =
            RegistryInventory.ITEM_MODULE_MIAPI_REGISTRY.get(Miapi.id("tm_archery:arrow/tail/fletching"));

    public FletchingTableScreenHandler(
            int containerId,
            Inventory playerInventory
    ) {
        this(Archery.FLETCHING_TABLE_SCREEN_PROVIDER, containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE));
    }

    public FletchingTableScreenHandler(
            @Nullable MenuType<?> menuType,
            int containerId,
            Inventory playerInventory,
            Container container
    ) {
        super(menuType, containerId);
        this.player = playerInventory.player;
        this.container = container;

        checkContainerSize(container, CONTAINER_SIZE);

        this.addSlot(new Slot(container, ARROW_INPUT_SLOT, 28, 35) {
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                slotsChanged(container);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemTags.ARROWS) || ModularItem.isModularItem(stack);
            }
        });


        // Input slots
        this.addSlot(new Slot(container, SHAFT_SLOT, 65, 35) {
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                slotsChanged(container);
            }
        });

        this.addSlot(new Slot(container, HEAD_SLOT, 65, 17) {
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                slotsChanged(container);
            }
        });

        this.addSlot(new Slot(container, TAIL_SLOT, 65, 53) {
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                slotsChanged(container);
            }
        });


        // Output slot
        this.addSlot(new Slot(container, OUTPUT_SLOT, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                performCraft();
                super.onTake(player, stack);
                preview();
            }
        });

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18
                ));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    8 + col * 18,
                    142
            ));
        }
    }

    private CraftActionPipelineArch buildPipeline() {
        ItemStack arrowItem = null;
        if (!container.getItem(0).isEmpty()) {
            arrowItem = container.getItem(0);
        }
        return CraftActionPipelineArch.create(player)
                .failureMode(CraftActionPipelineArch.FailureMode.NORMAL)
                .allowEmptyModules(true)
                .setAutoCount(true)
                .baseItem(arrowItem)

                // slot 0: shaft / base
                .add(shaftModule.id(), 1, container)

                // slot 1: head
                .add("head", headModule.id(), 2, container)

                // slot 2: tail
                .add("shaft", tailModule.id(), 3, container);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        for (int i = 0; i < 4; i++) {
            ItemStack stack = this.container.getItem(i);
            if (!stack.isEmpty()) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
                this.container.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    protected void preview() {
        CraftActionPipelineArch pipeline = buildPipeline();

        ItemStack result = pipeline.preview();

        container.setItem(
                OUTPUT_SLOT,
                result.isEmpty() ? ItemStack.EMPTY : result
        );
    }

    private void performCraft() {
        CraftActionPipelineArch pipeline = buildPipeline();
        ItemStack result = pipeline.perform();
        if (result.isEmpty()) {
            container.setItem(OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }
        ItemStack input = container.getItem(ARROW_INPUT_SLOT);
        int remaining = input.getCount() - result.getCount();
        if (remaining <= 0) {
            container.setItem(ARROW_INPUT_SLOT, ItemStack.EMPTY);
        } else {
            input.shrink(result.getCount());
            container.setItem(ARROW_INPUT_SLOT, input);
        }
        container.setChanged();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        preview();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);

        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem().copy();

        if (index == OUTPUT_SLOT) {
            ItemStack result = slot.getItem().copy();
            if (!this.moveItemStackTo(
                    result,
                    CONTAINER_SIZE,
                    this.slots.size(),
                    true
            )) {
                return ItemStack.EMPTY;
            }
            slot.set(result);
            performCraft();
            preview();

            return original;
        }

        if (index < INPUT_SLOTS) {
            if (!this.moveItemStackTo(
                    slot.getItem(),
                    CONTAINER_SIZE,
                    this.slots.size(),
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(
                    slot.getItem(),
                    0,
                    INPUT_SLOTS,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }

        if (slot.getItem().isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        preview();
        return original;
    }
}

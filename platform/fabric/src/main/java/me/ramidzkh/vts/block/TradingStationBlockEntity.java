package me.ramidzkh.vts.block;

import me.ramidzkh.vts.VillagerTradingStationFabric;
import me.ramidzkh.vts.item.QuoteItem;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TradingStationBlockEntity extends BlockEntity implements BlockEntityClientSerializable, ContainerListener {

    private final SimpleContainer inputs = new SimpleContainer(9);
    private final SimpleContainer outputs = new SimpleContainer(9);
    private final SimpleContainer quotes = new SimpleContainer(1);

    private final Storage<ItemVariant> inputStorage = InventoryStorage.of(inputs, null);
    private final Storage<ItemVariant> outputStorage = InventoryStorage.of(outputs, null);
    private final Storage<ItemVariant> quoteStorage = new FilteringStorage<>(InventoryStorage.of(quotes, null)) {
        @Override
        protected boolean canInsert(ItemVariant resource) {
            return resource.getItem() == VillagerTradingStationFabric.Items.QUOTE;
        }
    };
    private final Storage<ItemVariant> exposed = new CombinedStorage<>(List.of(
            new OneWayStorage<>(inputStorage, true, false),
            new OneWayStorage<>(outputStorage, false, true)
    ));

    public TradingStationBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);

        inputs.addListener(this);
        outputs.addListener(this);
        quotes.addListener(this);
    }

    public SimpleContainer getInputContainer() {
        return inputs;
    }

    public SimpleContainer getOutputContainer() {
        return outputs;
    }

    public SimpleContainer getQuoteContainer() {
        return quotes;
    }

    public Storage<ItemVariant> getStorage() {
        return exposed;
    }

    public Storage<ItemVariant> getQuotes() {
        return quoteStorage;
    }

    public void interact(AbstractVillager villager) {
        for (int i = 0; i < quotes.getContainerSize(); i++) {
            ItemStack stack = quotes.getItem(i);

            if (!(stack.getItem() instanceof QuoteItem quoteItem)) {
                continue;
            }

            QuoteItem.Quote quote = quoteItem.getQuote(stack);
            MerchantOffer myOffer = null;

            for (MerchantOffer offer : villager.getOffers()) {
                if (offer.isOutOfStock()) {
                    continue;
                }

                // MerchantOffer#satisfiedBy but exact amounts
                if (ItemStack.isSame(offer.getResult(), quote.result())
                    && offer.satisfiedBy(quote.a(), quote.b())
                    && offer.getCostA().getCount() == quote.a().getCount()
                    && offer.getCostB().getCount() == quote.b().getCount()) {
                    myOffer = offer;
                    break;
                }
            }

            if (myOffer != null) {
                try (Transaction transaction = Transaction.openOuter()) {
                    if (inputStorage.extract(ItemVariant.of(quote.a()), quote.a().getCount(), transaction) == quote.a().getCount()
                        && (quote.b().isEmpty() || inputStorage.extract(ItemVariant.of(quote.b()), quote.b().getCount(), transaction) == quote.b().getCount())
                        && outputStorage.insert(ItemVariant.of(quote.result()), quote.result().getCount(), transaction) == quote.result().getCount()) {
                        villager.notifyTrade(myOffer);
                        transaction.commit();
                    }
                }
            }
        }
    }

    public void drop(Level level, BlockPos blockPos) {
        Containers.dropContents(level, blockPos, inputs);
        Containers.dropContents(level, blockPos, outputs);
        Containers.dropContents(level, blockPos, quotes);
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);

        inputs.fromTag(compoundTag.getList("Inputs", NbtType.COMPOUND));
        outputs.fromTag(compoundTag.getList("Outputs", NbtType.COMPOUND));
        quotes.fromTag(compoundTag.getList("Quotes", NbtType.COMPOUND));
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        compoundTag.put("Inputs", inputs.createTag());
        compoundTag.put("Outputs", outputs.createTag());
        compoundTag.put("Quotes", quotes.createTag());

        return super.save(compoundTag);
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        return save(tag);
    }

    @Override
    public void containerChanged(Container container) {
        if (level instanceof ServerLevel) {
            setChanged();
        }
    }
}

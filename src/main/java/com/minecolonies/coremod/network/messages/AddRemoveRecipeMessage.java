package com.minecolonies.coremod.network.messages;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.IBuildingWorker;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.util.BlockPosUtil;
import com.ldtteam.structurize.util.LanguageHandler;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.coremod.colony.IColonyManager;
import com.minecolonies.coremod.colony.buildings.AbstractBuildingWorker;
import com.minecolonies.coremod.colony.buildings.views.AbstractBuildingView;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.minecolonies.api.util.constant.TranslationConstants.UNABLE_TO_ADD_RECIPE_MESSAGE;

/**
 * Message class to add and remove recipes.
 */
public class AddRemoveRecipeMessage implements IMessage
{
    /**
     * The Colony ID.
     */
    private int     colonyId;

    /**
     * Toggle the recipe allocation to remove or add.
     */
    private boolean remove;

    /**
     * The RecipeStorage to add/remove.
     */
    private IRecipeStorage storage;

    /**
     * The id of the building.
     */
    private BlockPos building;

    /**
     * The dimension of the 
     */
    private int dimension;

    /**
     * Create a message to add or remove recipes.
     * This constructor creates the recipeStorage on its own.
     * @param input the input.
     * @param gridSize the gridSize.
     * @param primaryOutput the primary output.
     * @param building the building.
     * @param remove true if remove.
     */
    public AddRemoveRecipeMessage(
            final List<ItemStack> input,
            final int gridSize,
            final ItemStack primaryOutput, final AbstractBuildingView building, final boolean remove)
    {
        super();
        if (gridSize == 1)
        {
            storage = StandardFactoryController.getInstance().getNewInstance(
              TypeConstants.RECIPE,
              StandardFactoryController.getInstance().getNewInstance(TypeConstants.ITOKEN),
              input,
              gridSize,
              primaryOutput, Blocks.FURNACE);
        }
        else
        {
            storage = StandardFactoryController.getInstance().getNewInstance(
              TypeConstants.RECIPE,
              StandardFactoryController.getInstance().getNewInstance(TypeConstants.ITOKEN),
              input,
              gridSize,
              primaryOutput);
        }
        this.remove = remove;
        this.dimension = building.getColony().getDimension();
        this.building = building.getPosition();
        this.colonyId = building.getColony().getID();
    }

    /**
     * Empty default constructor.
     */
    public AddRemoveRecipeMessage()
    {
        super();
    }

    /**
     * Create a message to add or remove recipes.
     * @param data the recipe storage.
     * @param building the building.
     * @param remove true if remove.
     */
    public AddRemoveRecipeMessage(final IRecipeStorage data, final AbstractBuildingView building, final boolean remove)
    {
        super();
        this.storage = data;
        this.remove = remove;
        this.building = building.getPosition();
        this.colonyId = building.getColony().getID();
        this.dimension = building.getColony().getDimension();
    }

    /**
     * Transformation from a byteStream.
     *
     * @param buf the used byteBuffer.
     */
    @Override
    public void fromBytes(@NotNull final PacketBuffer buf)
    {
        colonyId = buf.readInt();
        storage = StandardFactoryController.getInstance().readFromBuffer(buf);
        remove = buf.readBoolean();
        building = buf.readBlockPos();
        dimension = buf.readInt();
    }

    /**
     * Transformation to a byteStream.
     *
     * @param buf the used byteBuffer.
     */
    @Override
    public void toBytes(@NotNull final PacketBuffer buf)
    {
        buf.writeInt(colonyId);
        StandardFactoryController.getInstance().writeToBuffer(buf, storage);
        buf.writeBoolean(remove);
        buf.writeBlockPos(building);
        buf.writeInt(dimension);
    }

    @Nullable
    @Override
    public LogicalSide getExecutionSide()
    {
        return LogicalSide.SERVER;
    }
    
    @Override
    public void onExecute(final NetworkEvent.Context ctxIn, final boolean isLogicalServer)
    {
        final ServerPlayerEntity player = ctxIn.getSender();
        final IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyId, dimension);
        if (colony == null || !colony.getPermissions().hasPermission(player, Action.MANAGE_HUTS))
        {
            return;
        }

        final IBuilding buildingWorker = colony.getBuildingManager().getBuilding(building);
        if(buildingWorker instanceof AbstractBuildingWorker)
        {
            final IToken token = IColonyManager.getInstance().getRecipeManager().checkOrAddRecipe(storage);

            if(remove)
            {
                ((IBuildingWorker) buildingWorker).removeRecipe(token);
            }
            else
            {
                if (!((IBuildingWorker) buildingWorker).addRecipe(token))
                {
                    LanguageHandler.sendPlayerMessage(player, UNABLE_TO_ADD_RECIPE_MESSAGE, ((IBuildingWorker) buildingWorker).getJobName());
                }
                else
                {
                    LanguageHandler.sendPlayerMessage(player, "com.minecolonies.coremod.gui.recipe.done");
                }
            }

            buildingWorker.markDirty();


        }
    }
}

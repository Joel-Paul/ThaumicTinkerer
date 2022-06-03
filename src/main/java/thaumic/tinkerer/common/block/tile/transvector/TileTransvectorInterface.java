/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the ThaumicTinkerer Mod.
 *
 * ThaumicTinkerer is Open Source and distributed under a
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 License
 * (http://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB)
 *
 * ThaumicTinkerer is a Derivative Work on Thaumcraft 4.
 * Thaumcraft 4 (c) Azanor 2012
 * (http://www.minecraftforum.net/topic/1585216-)
 *
 * File Created @ [8 Sep 2013, 19:01:20 (GMT)]
 */
package thaumic.tinkerer.common.block.tile.transvector;


import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import ic2.api.energy.tile.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.TileEssentiaReservoir;
import thaumcraft.common.tiles.TileJarFillable;
import thaumcraft.common.tiles.TileThaumatorium;
import thaumcraft.common.tiles.TileThaumatoriumTop;
import thaumic.tinkerer.common.compat.IndustrialcraftUnloadHelper;
import thaumic.tinkerer.common.lib.LibFeatures;

@Optional.InterfaceList({@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft"),
        @Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHCore"),
        @Optional.Interface(iface = "cofh.api.energy.IEnergyReceiver", modid = "CoFHCore"),
        @Optional.Interface(iface = "cofh.api.energy.IEnergyProvider", modid = "CoFHCore"),
        @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2"),
        @Optional.Interface(iface = "ic2.api.energy.tile.IEnergyEmitter", modid = "IC2"),
        @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "IC2"),
        @Optional.Interface(iface = "ic2.api.energy.tile.IEnergyConductor", modid = "IC2")})

public class TileTransvectorInterface extends TileTransvector implements ISidedInventory, IEnergyEmitter,IEnergySink,IEnergyConductor,IEnergySource, IFluidHandler, IEnergyHandler, IEnergyReceiver, IAspectContainer, IEssentiaTransport, IPeripheral,IEnergyProvider {

    public boolean addedToICEnergyNet = false;
    private int jarCount = 0;
    private int reservoirCount = 0;

    public static int[] buildSlotsForLinearInventory(IInventory inv) {
        int[] slots = new int[inv.getSizeInventory()];
        for (int i = 0; i < slots.length; i++)
            slots[i] = i;

        return slots;
    }

    @Override
    public void updateEntity() {
        if (worldObj.getTotalWorldTime() % 100 == 0) {
            worldObj.notifyBlockChange(xCoord, yCoord, zCoord, worldObj.getBlock(xCoord, yCoord, zCoord));
        }
        if (!addedToICEnergyNet && !worldObj.isRemote && Loader.isModLoaded("IC2")) {

            IndustrialcraftUnloadHelper.addToIC2EnergyNet(this);
            addedToICEnergyNet = true;
        }
        TileEntity tile = getTile();
        if (tile instanceof IEssentiaTransport) {
            drawEssentia(tile);
        }
    }

    // Essentia input is hardcoded, so these need to be accounted for manually.
    private void drawEssentia(TileEntity tile) {
        if (tile instanceof TileEssentiaReservoir) {
            fillReservoir((TileEssentiaReservoir) tile);
        }
        else if (tile instanceof TileJarFillable) {
            fillJar((TileJarFillable) tile);
        }
        else if (tile instanceof TileThaumatorium) {
            fillThaumatorium((TileThaumatorium) tile);
        }
        else if (tile instanceof TileThaumatoriumTop) {
            fillThaumatorium(((TileThaumatoriumTop) tile).thaumatorium);
        }
    }

    private void fillReservoir(TileEssentiaReservoir reservoir) {
        ++this.reservoirCount;
        if(!this.worldObj.isRemote && this.reservoirCount % 5 == 0 && reservoir.essentia.visSize() < reservoir.maxAmount) {
            TileEntity te = ThaumcraftApiHelper.getConnectableTile(this.worldObj, this.xCoord, this.yCoord, this.zCoord, reservoir.facing);
            if(te != null) {
                IEssentiaTransport ic = (IEssentiaTransport)te;
                if(!ic.canOutputTo(reservoir.facing.getOpposite())) {
                    return;
                }

                Aspect ta = null;
                if(ic.getEssentiaAmount(reservoir.facing.getOpposite()) > 0 && ic.getSuctionAmount(reservoir.facing.getOpposite()) < this.getSuctionAmount(reservoir.facing) && this.getSuctionAmount(reservoir.facing) >= ic.getMinimumSuction()) {
                    ta = ic.getEssentiaType(reservoir.facing.getOpposite());
                }

                if(ta != null && ic.getSuctionAmount(reservoir.facing.getOpposite()) < this.getSuctionAmount(reservoir.facing)) {
                    this.addToContainer(ta, ic.takeEssentia(ta, 1, reservoir.facing.getOpposite()));
                }
            }
        }
    }

    private void fillJar(TileJarFillable jar) {
        if (!this.worldObj.isRemote && ++this.jarCount % 5 == 0 && jar.amount < jar.maxAmount) {
            TileEntity te = ThaumcraftApiHelper.getConnectableTile(this.worldObj, this.xCoord, this.yCoord, this.zCoord, ForgeDirection.UP);
            if(te != null) {
                IEssentiaTransport ic = (IEssentiaTransport)te;
                if(!ic.canOutputTo(ForgeDirection.DOWN)) {
                    return;
                }

                Aspect ta = null;
                if(jar.aspectFilter != null) {
                    ta = jar.aspectFilter;
                } else if(jar.aspect != null && jar.amount > 0) {
                    ta = jar.aspect;
                } else if(ic.getEssentiaAmount(ForgeDirection.DOWN) > 0 && ic.getSuctionAmount(ForgeDirection.DOWN) < this.getSuctionAmount(ForgeDirection.UP) && this.getSuctionAmount(ForgeDirection.UP) >= ic.getMinimumSuction()) {
                    ta = ic.getEssentiaType(ForgeDirection.DOWN);
                }

                if(ta != null && ic.getSuctionAmount(ForgeDirection.DOWN) < this.getSuctionAmount(ForgeDirection.UP)) {
                    this.addToContainer(ta, ic.takeEssentia(ta, 1, ForgeDirection.DOWN));
                }
            }
        }
    }

    private void fillThaumatorium(TileThaumatorium thaumatorium) {
        if (!this.worldObj.isRemote && thaumatorium.currentSuction != null) {
            TileEntity te = null;
            IEssentiaTransport ic = null;

            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                if (dir != thaumatorium.facing && dir != ForgeDirection.DOWN) {
                    te = ThaumcraftApiHelper.getConnectableTile(this.worldObj, this.xCoord, this.yCoord, this.zCoord, dir);
                    if (te != null) {
                        ic = (IEssentiaTransport) te;
                        if (ic.getEssentiaAmount(dir.getOpposite()) > 0 && ic.getSuctionAmount(dir.getOpposite()) < this.getSuctionAmount((ForgeDirection) null) && this.getSuctionAmount((ForgeDirection) null) >= ic.getMinimumSuction()) {
                            int ess = ic.takeEssentia(thaumatorium.currentSuction, 1, dir.getOpposite());
                            if (ess > 0) {
                                this.addToContainer(thaumatorium.currentSuction, ess);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getMaxDistance() {
        return LibFeatures.INTERFACE_DISTANCE;
    }

    @Override
    public void invalidate() {
        if (Loader.isModLoaded("IC2")) {
            IndustrialcraftUnloadHelper.removeFromIC2EnergyNet(this);
        }
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {

        if (Loader.isModLoaded("IC2")) {
            IndustrialcraftUnloadHelper.removeFromIC2EnergyNet(this);
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        TileEntity tile = getTile();
        if (tile != null)
            tile.markDirty();
    }

    @Override
    public int getSizeInventory() {
        TileEntity tile = getTile();
        return tile instanceof IInventory ? ((IInventory) tile).getSizeInventory() : 0;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        TileEntity tile = getTile();
        return tile instanceof IInventory ? ((IInventory) tile).getStackInSlot(i) : null;
    }

    @Override
    public ItemStack decrStackSize(int i, int j) {
        TileEntity tile = getTile();
        return tile instanceof IInventory ? ((IInventory) tile).decrStackSize(i, j) : null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i) {
        TileEntity tile = getTile();
        return tile instanceof IInventory ? ((IInventory) tile).getStackInSlotOnClosing(i) : null;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        TileEntity tile = getTile();
        if (tile instanceof IInventory)
            ((IInventory) tile).setInventorySlotContents(i, itemstack);
    }

    @Override
    public String getInventoryName() {
        TileEntity tile = getTile();
        return tile instanceof IInventory ? ((IInventory) tile).getInventoryName() : "";
    }

    @Override
    public boolean hasCustomInventoryName() {
        TileEntity tile = getTile();
        return tile instanceof IInventory && ((IInventory) tile).hasCustomInventoryName();
    }

    @Override
    public int getInventoryStackLimit() {
        TileEntity tile = getTile();
        return tile instanceof IInventory ? ((IInventory) tile).getInventoryStackLimit() : 0;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer) {
        TileEntity tile = getTile();
        return tile instanceof IInventory && ((IInventory) tile).isUseableByPlayer(entityplayer);
    }

    @Override
    public void openInventory() {
        TileEntity tile = getTile();
        if (tile instanceof IInventory)
            ((IInventory) tile).openInventory();
    }

    @Override
    public void closeInventory() {
        TileEntity tile = getTile();
        if (tile instanceof IInventory)
            ((IInventory) tile).closeInventory();
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        TileEntity tile = getTile();
        return tile instanceof IInventory && ((IInventory) tile).isItemValidForSlot(i, itemstack);
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        TileEntity tile = getTile();
        return tile instanceof IFluidHandler ? ((IFluidHandler) tile).fill(from, resource, doFill) : 0;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        TileEntity tile = getTile();
        return tile instanceof IFluidHandler ? ((IFluidHandler) tile).drain(from, resource, doDrain) : null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        TileEntity tile = getTile();
        return tile instanceof IFluidHandler ? ((IFluidHandler) tile).drain(from, maxDrain, doDrain) : null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        TileEntity tile = getTile();
        return tile instanceof IFluidHandler && ((IFluidHandler) tile).canFill(from, fluid);
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        TileEntity tile = getTile();
        return tile instanceof IFluidHandler && ((IFluidHandler) tile).canDrain(from, fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        TileEntity tile = getTile();
        return tile instanceof IFluidHandler ? ((IFluidHandler) tile).getTankInfo(from) : null;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int var1) {
        TileEntity tile = getTile();
        return tile instanceof ISidedInventory ? ((ISidedInventory) tile).getAccessibleSlotsFromSide(var1) : tile instanceof IInventory ? buildSlotsForLinearInventory((IInventory) tile) : new int[0];
    }

    @Override
    public boolean canInsertItem(int i, ItemStack itemstack, int j) {
        TileEntity tile = getTile();
        return tile instanceof ISidedInventory ? ((ISidedInventory) tile).canInsertItem(i, itemstack, j) : tile instanceof IInventory;
    }

    @Override
    public boolean canExtractItem(int i, ItemStack itemstack, int j) {
        TileEntity tile = getTile();
        return tile instanceof ISidedInventory ? ((ISidedInventory) tile).canExtractItem(i, itemstack, j) : tile instanceof IInventory;
    }



    @Optional.Method(modid = "IC2")
    @Override
    public double getDemandedEnergy() {
        TileEntity tile = getTile();
        return tile instanceof IEnergySink ? ((IEnergySink) tile).getDemandedEnergy() : 0;
    }


    @Optional.Method(modid = "IC2")
    @Override
    public double injectEnergy(ForgeDirection directionFrom, double amount, double voltage) {

        TileEntity tile = getTile();
        return tile instanceof IEnergySink ? ((IEnergySink) tile).injectEnergy(directionFrom, amount, voltage) : 0;
    }


    @Optional.Method(modid = "IC2")
    @Override
    public int getSinkTier() {
        TileEntity tile = getTile();
        return tile instanceof IEnergySink ? ((IEnergySink) tile).getSinkTier() : 0;
    }


    @Override
    @Optional.Method(modid = "CoFHLib")
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        TileEntity tile = getTile();
        return tile instanceof IEnergyHandler ? ((IEnergyHandler) tile).receiveEnergy(from, maxReceive, simulate) : 0;
    }

    @Override
    @Optional.Method(modid = "CoFHLib")
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        TileEntity tile = getTile();
        return tile instanceof IEnergyHandler ? ((IEnergyHandler) tile).extractEnergy(from, maxExtract, simulate) : 0;
    }


    @Override
    @Optional.Method(modid = "CoFHLib")
    public int getEnergyStored(ForgeDirection from) {
        TileEntity tile = getTile();
        return tile instanceof IEnergyHandler ? ((IEnergyHandler) tile).getEnergyStored(from) : 0;
    }

    @Override
    @Optional.Method(modid = "CoFHLib")
    public int getMaxEnergyStored(ForgeDirection from) {
        TileEntity tile = getTile();
        return tile instanceof IEnergyHandler ? ((IEnergyHandler) tile).getMaxEnergyStored(from) : 0;
    }

    @Override
    @Optional.Method(modid = "CoFHLib")
    public boolean canConnectEnergy(ForgeDirection from) {
        TileEntity tile = getTile();
        return tile instanceof IEnergyHandler ? ((IEnergyHandler) tile).canConnectEnergy(from) : false;
    }

    @Override
    public AspectList getAspects() {
        TileEntity tile = getTile();
        return tile instanceof IAspectContainer ? ((IAspectContainer) tile).getAspects() : null;
    }

    @Override
    public void setAspects(AspectList paramAspectList) {
        TileEntity tile = getTile();
        if (tile != null)
            ((IAspectContainer) tile).setAspects(paramAspectList);
    }

    @Override
    public boolean doesContainerAccept(Aspect paramAspect) {
        TileEntity tile = getTile();
        return tile instanceof IAspectContainer && ((IAspectContainer) tile).doesContainerAccept(paramAspect);
    }

    @Override
    public int addToContainer(Aspect paramAspect, int paramInt) {
        TileEntity tile = getTile();
        return tile instanceof IAspectContainer ? ((IAspectContainer) tile).addToContainer(paramAspect, paramInt) : 0;
    }

    @Override
    public boolean takeFromContainer(Aspect paramAspect, int paramInt) {
        TileEntity tile = getTile();
        return tile instanceof IAspectContainer && ((IAspectContainer) tile).takeFromContainer(paramAspect, paramInt);
    }

    @Override
    public boolean takeFromContainer(AspectList paramAspectList) {
        TileEntity tile = getTile();
        return tile instanceof IAspectContainer && ((IAspectContainer) tile).takeFromContainer(paramAspectList);
    }

    @Override
    public boolean doesContainerContainAmount(Aspect paramAspect, int paramInt) {
        TileEntity tile = getTile();
        return tile instanceof IAspectContainer && ((IAspectContainer) tile).doesContainerContainAmount(paramAspect, paramInt);
    }

    @Override
    public boolean doesContainerContain(AspectList paramAspectList) {
        TileEntity tile = getTile();
        return tile instanceof IAspectContainer && ((IAspectContainer) tile).doesContainerContain(paramAspectList);
    }

    @Override
    public int containerContains(Aspect paramAspect) {
        TileEntity tile = getTile();
        return tile instanceof IAspectContainer ? ((IAspectContainer) tile).containerContains(paramAspect) : 0;
    }

    @Override
    public boolean isConnectable(ForgeDirection forgeDirection) {
        TileEntity tile = getTile();
        return tile instanceof IEssentiaTransport && ((IEssentiaTransport) tile).isConnectable(forgeDirection);
//    	return true;
    }

    @Override
    public boolean canInputFrom(ForgeDirection forgeDirection) {
        TileEntity tile = getTile();
        return tile instanceof IEssentiaTransport && ((IEssentiaTransport) tile).canInputFrom(forgeDirection);
    }

    @Override
    public boolean canOutputTo(ForgeDirection forgeDirection) {
        TileEntity tile = getTile();
        return tile instanceof IEssentiaTransport && ((IEssentiaTransport) tile).canOutputTo(forgeDirection);
    }

    @Override
    public void setSuction(Aspect paramAspect, int paramInt) {
        TileEntity tile = getTile();
        if (tile instanceof IEssentiaTransport)
            ((IEssentiaTransport) tile).setSuction(paramAspect, paramInt);
    }

    @Override
    public Aspect getSuctionType(ForgeDirection forgeDirection) {
    	TileEntity tile = getTile();
        if (tile instanceof IEssentiaTransport)
            return ((IEssentiaTransport) tile).getSuctionType(forgeDirection);
        return null;
    }

    @Override
    public int getSuctionAmount(ForgeDirection forgeDirection) {
    	TileEntity tile = getTile();
        if (tile instanceof IEssentiaTransport)
            return ((IEssentiaTransport) tile).getSuctionAmount(forgeDirection);
        return 0;
    }

    @Override
    public int takeEssentia(Aspect paramAspect, int paramInt, ForgeDirection forgeDirection) {
        TileEntity tile = getTile();
        return tile instanceof IEssentiaTransport ? ((IEssentiaTransport) tile).takeEssentia(paramAspect, paramInt, forgeDirection) : 0;
    }

    @Override
    public int getMinimumSuction() {
        TileEntity tile = getTile();
        return tile instanceof IEssentiaTransport ? ((IEssentiaTransport) tile).getMinimumSuction() : 0;
    }

    @Override
    public boolean renderExtendedTube() {
    	TileEntity tile = getTile();
        return tile instanceof IEssentiaTransport && ((IEssentiaTransport) tile).renderExtendedTube();
    }

    @Override
    public int addEssentia(Aspect arg0, int arg1, ForgeDirection forgeDirection) {
        TileEntity tile = getTile();
        return tile instanceof IEssentiaTransport ? ((IEssentiaTransport) tile).addEssentia(arg0, arg1, forgeDirection) : 0;
    }

    @Override
    public Aspect getEssentiaType(ForgeDirection forgeDirection) {

        TileEntity tile = getTile();
        return tile instanceof IEssentiaTransport ? ((IEssentiaTransport) tile).getEssentiaType(forgeDirection) : null;
    }

    @Override
    public int getEssentiaAmount(ForgeDirection forgeDirection) {
        TileEntity tile = getTile();
        return tile instanceof IEssentiaTransport ? ((IEssentiaTransport) tile).getEssentiaAmount(forgeDirection) : 0;
    }

    @Override
    @Optional.Method(modid = "ComputerCraft")
    public String getType() {
        return getTile() instanceof IPeripheral ? ((IPeripheral) getTile()).getType() : "Transvector Interface Unconnected Peripherad";
    }

    @Override
    @Optional.Method(modid = "ComputerCraft")
    public String[] getMethodNames() {
        return getTile() instanceof IPeripheral ? ((IPeripheral) getTile()).getMethodNames() : new String[0];
    }

    @Override
    @Optional.Method(modid = "ComputerCraft")
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws LuaException, InterruptedException {
        return getTile() instanceof IPeripheral ? ((IPeripheral) getTile()).callMethod(computer, context, method, arguments) : new Object[0];
    }

    @Override

    public void attach(IComputerAccess computer) {
        if (getTile() instanceof IPeripheral) {
            ((IPeripheral) getTile()).attach(computer);
        }
    }

    @Override
    @Optional.Method(modid = "ComputerCraft")
    public void detach(IComputerAccess computer) {
        if (getTile() instanceof IPeripheral) {
            ((IPeripheral) getTile()).detach(computer);
        }
    }

    @Override
    @Optional.Method(modid = "ComputerCraft")
    public boolean equals(IPeripheral other) {
        return this.equals((Object) other);
    }


    @Optional.Method(modid = "IC2")
    @Override
    public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction) {
        TileEntity tile = getTile();
        return tile instanceof IEnergyAcceptor ? ((IEnergySink) tile).acceptsEnergyFrom(emitter, direction) : false;
    }

    @Override
    @Optional.Method(modid = "IC2")
    public double getConductionLoss() {
        TileEntity tile = getTile();
        return tile instanceof IEnergyConductor ? ((IEnergyConductor)tile).getConductionLoss():0;
    }

    @Override
    @Optional.Method(modid = "IC2")
    public double getInsulationEnergyAbsorption() {
        TileEntity tile = getTile();
        return tile instanceof IEnergyConductor ? ((IEnergyConductor)tile).getInsulationEnergyAbsorption():0;
    }

    @Override
    @Optional.Method(modid = "IC2")
    public double getInsulationBreakdownEnergy() {
        TileEntity tile = getTile();
        return tile instanceof IEnergyConductor ? ((IEnergyConductor)tile).getInsulationBreakdownEnergy():0;
    }

    @Override
    @Optional.Method(modid = "IC2")
    public double getConductorBreakdownEnergy() {
        TileEntity tile = getTile();
        return tile instanceof IEnergyConductor ? ((IEnergyConductor)tile).getConductorBreakdownEnergy():0;
    }

    @Override
    @Optional.Method(modid = "IC2")
    public void removeInsulation() {
        TileEntity tile = getTile();
        if(tile instanceof IEnergyConductor )
            ((IEnergyConductor)tile).removeInsulation();
    }


    @Override
    @Optional.Method(modid = "IC2")
    public void removeConductor() {

        TileEntity tile = getTile();
        if(tile instanceof IEnergyConductor )
            ((IEnergyConductor)tile).removeConductor();
    }

    @Override
    @Optional.Method(modid = "IC2")
    public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
        TileEntity tile = getTile();
        return tile instanceof IEnergyEmitter ? ((IEnergyEmitter)tile).emitsEnergyTo(receiver,direction):false;
    }

    @Override
    @Optional.Method(modid = "IC2")
    public double getOfferedEnergy() {
        TileEntity tile = getTile();
        return tile instanceof IEnergySource ? ((IEnergySource)tile).getOfferedEnergy():0;
    }

    @Override
    @Optional.Method(modid = "IC2")
    public void drawEnergy(double amount) {
        TileEntity tile = getTile();
        if(tile instanceof IEnergySource )
            ((IEnergySource)tile).drawEnergy(amount);
    }

    @Override
    @Optional.Method(modid = "IC2")
    public int getSourceTier() {
        TileEntity tile = getTile();
        return tile instanceof IEnergySource ? ((IEnergySource)tile).getSourceTier():0;
    }
}

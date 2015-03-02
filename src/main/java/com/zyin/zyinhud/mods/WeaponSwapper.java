package com.zyin.zyinhud.mods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;

import com.google.common.collect.Multimap;
import com.zyin.zyinhud.ZyinHUDRenderer;
import com.zyin.zyinhud.util.Localization;
import com.zyin.zyinhud.util.ModCompatibility;

/**
 * Weapon Swap allows the player to quickly equip their sword and bow.
 */
public class WeaponSwapper extends ZyinHUDModBase
{
	/** Enables/Disables this Mod */
	public static boolean Enabled;

    /**
     * Toggles this Mod on or off
     * @return The state the Mod was changed to
     */
    public static boolean ToggleEnabled()
    {
    	return Enabled = !Enabled;
    }
    
    //private static List<Class> meleeWeaponClasses = null;
    private static List<Class> rangedWeaponClasses = null;
    

    /**
     * Makes the player select their sword. If a sword is already selected, it selects the bow instead.
     */
    public static void SwapWeapons()
    {
        ItemStack currentItemStack = mc.thePlayer.getHeldItem();
        Item currentItem = null;

        if (currentItemStack != null)
        {
            currentItem = currentItemStack.getItem();
        }

        InitializeListOfWeaponClasses();
        

        int meleeWeaponSlot = GetMostDamagingWeaponSlot();
        int rangedWeaponSlot = GetItemSlotFromHotbar(rangedWeaponClasses);

        if (meleeWeaponSlot < 0 && rangedWeaponSlot < 0)
        {
            //we dont have a sword or a bow
        	ZyinHUDRenderer.DisplayNotification(Localization.get("weaponswapper.noweaponsinhotbar"));
        }
        else if (meleeWeaponSlot >= 0 && rangedWeaponSlot < 0)
        {
            //we have a sword, but no bow
            SelectHotbarSlot(meleeWeaponSlot);
        }
        else if (meleeWeaponSlot < 0 && rangedWeaponSlot >= 0)
        {
            //we have a bow, but no sword
            SelectHotbarSlot(rangedWeaponSlot);
        }
        else
        {
        	//we have both a bow and a sword
        	if(mc.thePlayer.inventory.currentItem == meleeWeaponSlot)
        	{
        		//we are selected on the best melee weapon, so select the ranged weapon
        		SelectHotbarSlot(rangedWeaponSlot);
        	}
        	else
        	{
                //we are not selecting the best melee weapon, so select the melee weapon
                SelectHotbarSlot(meleeWeaponSlot);
        	}
        }
    }
    
    /**
     * Gets the hotbar index of the most damaging melee weapon on the hotbar.
     * @return 0-9
     */
    public static int GetMostDamagingWeaponSlot()
    {
        ItemStack[] items = mc.thePlayer.inventory.mainInventory;
        double highestWeapopnDamage = -1;
        int highestWeapopnDamageSlot = -1;
        
        for (int i = 0; i < 9; i++)
        {
            ItemStack itemStack = items[i];

            if (itemStack != null)
            {
                double weaponDamage = GetItemWeaponDamage(itemStack);
                if(weaponDamage > highestWeapopnDamage)
                {
                	highestWeapopnDamage = weaponDamage;
                	highestWeapopnDamageSlot = i;
                }
            }
        }
        return highestWeapopnDamageSlot;
    }
    
    /**
     * Gets the amount of melee damage delt by the specified item
     * @param itemStack
     * @return -1 if it doesn't have a damage modifier
     */
    public static double GetItemWeaponDamage(ItemStack itemStack)
    {
		Multimap multimap = itemStack.getItem().getAttributeModifiers(itemStack);
		
		if (multimap.containsKey(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName()))
		{
			Collection attributes = multimap.get(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName());
			if (attributes.size() > 0)
			{
				Object attribute = attributes.iterator().next();
				if (attribute instanceof AttributeModifier)
				{
					AttributeModifier weaponModifier = (AttributeModifier)attribute;
					return weaponModifier.getAmount();
				}
			}
		}
		return -1;
    }
    
	private static void InitializeListOfWeaponClasses()
	{
        if(rangedWeaponClasses == null)
        {
        	rangedWeaponClasses = new ArrayList<Class>();
        	rangedWeaponClasses.add(ItemBow.class);
        	
            if(ModCompatibility.TConstruct.isLoaded)
            {
    			try
    			{
    	        	rangedWeaponClasses.add(Class.forName(ModCompatibility.TConstruct.tConstructBowClass));
    			}
    			catch (ClassNotFoundException e)
    			{
    				e.printStackTrace();
    			}
            }
        }
	}
    
    /**
     * Determines if an item is a melee weapon.
     * @param item
     * @return
     */
    /*
	private static boolean IsMeleeWeapon(Item item)
    {
    	if(meleeWeaponClasses == null)
    		return false;
    	
        for(int j = 0; j < meleeWeaponClasses.size(); j++)
        {
            if (meleeWeaponClasses.get(j).isInstance(item))
            {
                return true;
            }
        }
		return false;
	}
    */
    
    /**
     * Determines if an item is a melee weapon.
     * @param item
     * @return
     */
    private static boolean IsRangedWeapon(Item item)
    {
    	if(rangedWeaponClasses == null)
    		return false;
    	
        for(int j = 0; j < rangedWeaponClasses.size(); j++)
        {
            if (rangedWeaponClasses.get(j).isInstance(item))
            {
                return true;
            }
        }
		return false;
	}

	/**
     * Makes the player select a slot on their hotbar
     * @param slot 0 through 8
     */
    protected static void SelectHotbarSlot(int slot)
    {
        if (slot < 0 || slot > 8)
        {
            return;
        }

        mc.thePlayer.inventory.currentItem = slot;
    }


    /**
     * Gets the index of an item that exists in the player's hotbar.
     * @param itemClasses the type of item to find (i.e. ItemSword.class, ItemBow.class)
     * @return 0 through 8, inclusive. -1 if not found.
     */
    protected static int GetItemSlotFromHotbar(List<Class> itemClasses)
    {
        ItemStack[] items = mc.thePlayer.inventory.mainInventory;

        for (int i = 0; i < 9; i++)
        {
            ItemStack itemStack = items[i];

            if (itemStack != null)
            {
                Item item = itemStack.getItem();
                
                for(int j = 0; j < itemClasses.size(); j++)
                {
                    if (itemClasses.get(j).isInstance(item))
                    {
                        return i;
                    }
                }
            }
        }
            
        return -1;
    }
}

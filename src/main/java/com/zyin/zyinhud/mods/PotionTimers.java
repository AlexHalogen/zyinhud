package com.zyin.zyinhud.mods;

import java.util.Collection;
import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.zyin.zyinhud.gui.GuiZyinHUDOptions;
import com.zyin.zyinhud.util.Localization;
import com.zyin.zyinhud.util.ZyinHUDUtil;
import cpw.mods.fml.common.ObfuscationReflectionHelper;

/**
 * Potion Timers displays the remaining time left on any potion effects the user has.
 */
public class PotionTimers
{
	/** Enables/Disables this Mod */
	public static boolean Enabled;

    /**
     * Toggles this Mod on or off
     * @return The state the Mod was changed to
     */
    public static boolean ToggleEnabled()
    {
    	Enabled = !Enabled;
    	return Enabled;
    }
    
    private static Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation inventoryResourceLocation = new ResourceLocation("textures/gui/container/inventory.png");
    
    public static boolean ShowPotionIcons;
    public static boolean UsePotionColors;
    public static float PotionScale;
    public static boolean HidePotionEffectsInInventory;

    protected static final int[] blinkingThresholds = {3 * 20, 6 * 20, 16 * 20};	//the time at which blinking starts
    protected static final int[] blinkingSpeed = {5, 10, 20};					//how often the blinking occurs
    protected static final int[] blinkingDuration = {2, 3, 3};					//how long the blink lasts

    protected static int potionLocX = 1;
    protected static int potionLocY = 16;

    /**
     * Renders the duration any potion effects that the player currently has on the left side of the screen.
     */
    public static void RenderOntoHUD()
    {
        //if the player is in the world
        //and not in a menu (except for chat and the custom Options menu)
        //and F3 not shown
        if (PotionTimers.Enabled &&
                mc.inGameHasFocus ||
                (mc.currentScreen != null && (mc.currentScreen instanceof GuiChat || TabIsSelectedInOptionsGui())) &&
        		!mc.gameSettings.showDebugInfo)
        {
            Collection potionEffects = mc.thePlayer.getActivePotionEffects();	//key:potionId, value:potionEffect
            Iterator it = potionEffects.iterator();
            
            int x = potionLocX;
            int y = potionLocY;
            
            x /= PotionScale;
            y /= PotionScale;
            GL11.glScalef(PotionScale, PotionScale, PotionScale);
            

            int i = 0;
            while (it.hasNext())
            {
                PotionEffect potionEffect = (PotionEffect)it.next();
                Potion potion = Potion.potionTypes[potionEffect.getPotionID()];
                Boolean isFromBeacon = potionEffect.getIsAmbient();	//Minecraft bug: this is always false

                if (!isFromBeacon)	//ignore effects from Beacons (Minecraft bug: isFromBeacon is always false)
                {
                	if(ShowPotionIcons)
                	{
                		DrawPotionIcon(x, y, potion);
                		DrawPotionDuration(x+10, y, potion, potionEffect);
                	}
                	else
                		DrawPotionDuration(x, y, potion, potionEffect);

                	y += 10;
                    i++;
                }
            }

            GL11.glScalef(1f/PotionScale, 1f/PotionScale, 1f/PotionScale);
        }
    }


    /**
     * Draws a potion's remaining duration with a color coded blinking timer
     * @param x
     * @param y
     * @param potion
     * @param potionEffect
     */
	protected static void DrawPotionDuration(int x, int y, Potion potion, PotionEffect potionEffect)
	{
		String durationString = Potion.getDurationString(potionEffect);
		int potionDuration = potionEffect.getDuration();	//goes down by 20 ticks per second
		int colorInt = 0xFFFFFF;
		
		if(UsePotionColors)
			colorInt = potion.getLiquidColor();
		
		
		boolean unicodeFlag = mc.fontRenderer.getUnicodeFlag();
        mc.fontRenderer.setUnicodeFlag(true);
		
		//render the potion duration text onto the screen
		if (potionDuration >= blinkingThresholds[blinkingThresholds.length - 1])	//if the text is not blinking then render it normally
		{
		    mc.fontRenderer.drawStringWithShadow(durationString, x, y, colorInt);
		}
		else //else if the text is blinking, have a chance to not render it based on the blinking variables
		{
			//logic to determine if the text should be displayed, checks the blinking text settings
		    for (int j = 0; j < blinkingThresholds.length; j++)
		    {
		        if (potionDuration < blinkingThresholds[j])
		        {
		            if (potionDuration % blinkingSpeed[j] > blinkingDuration[j])
		            {
		                mc.fontRenderer.drawStringWithShadow(durationString, x, y, colorInt);
		            }

		            break;
		        }
		    }
		}

        mc.fontRenderer.setUnicodeFlag(unicodeFlag);
	}
    
    /**
     * Draws a potion's icon texture
     * @param x
     * @param y
     * @param potion
     */
    protected static void DrawPotionIcon(int x, int y, Potion potion)
    {
        int iconIndex = potion.getStatusIconIndex();
        int u = iconIndex % 8 * 18;
        int v = 198 + iconIndex / 8 * 18;
        int width = 18;
        int height = 18;
        float scaler = 0.5f;
        
        GL11.glColor4f(1f, 1f, 1f, 1f);
        
    	ZyinHUDUtil.DrawTexture(x, y, u, v, width, height, inventoryResourceLocation, scaler);
    }
    
    /**
     * Disables the potion effects from rendering by telling the Gui that the player has no potion effects applied.
     * Uses reflection to grab the class's private variable.
     * @param guiScreen the screen the player is looking at which extends InventoryEffectRenderer
     */
    public static void DisableInventoryPotionEffects(InventoryEffectRenderer guiScreen)
    {
    	if(PotionTimers.Enabled && HidePotionEffectsInInventory)
    	{
    		//Note for future Forge versions: field "field_147045_u" will probably be renamed to something like "playerHasPotionEffects"
	    	boolean playerHasPotionEffects = ObfuscationReflectionHelper.getPrivateValue(InventoryEffectRenderer.class, (InventoryEffectRenderer)guiScreen, "field_147045_u");
	    	
	    	if(playerHasPotionEffects)
	    	{
	    		int guiLeftPx = (guiScreen.width - 176) / 2;
	    		
	    		ObfuscationReflectionHelper.setPrivateValue(GuiContainer.class, (GuiContainer)guiScreen, guiLeftPx, "field_147003_i","guiLeft");
	        	ObfuscationReflectionHelper.setPrivateValue(InventoryEffectRenderer.class, (InventoryEffectRenderer)guiScreen, false, "field_147045_u");
	    	}
    	}
    }
    
    
    /**
     * Checks to see if the Potion Timers tab is selected in GuiZyinHUDOptions
     * @return
     */
    private static boolean TabIsSelectedInOptionsGui()
    {
    	return mc.currentScreen instanceof GuiZyinHUDOptions &&
    		(((GuiZyinHUDOptions)mc.currentScreen).IsButtonTabSelected(Localization.get("potiontimers.name")));
    }
    

    /**
     * Toggles showing potion icons
     * @return 
     */
    public static boolean ToggleShowPotionIcons()
    {
    	ShowPotionIcons = !ShowPotionIcons;
    	return ShowPotionIcons;
    }
    
    /**
     * Toggles using potion colors
     * @return 
     */
    public static boolean ToggleUsePotionColors()
    {
    	UsePotionColors = !UsePotionColors;
    	return UsePotionColors;
    }
    
    /**
     * Toggles hiding potion effects in the players inventory
     * @return 
     */
    public static boolean ToggleHidePotionEffectsInInventory()
    {
    	HidePotionEffectsInInventory = !HidePotionEffectsInInventory;
    	return HidePotionEffectsInInventory;
    }
    
    /**
     * Gets the horizontal location where the potion timers are rendered.
     * @return
     */
    public static int GetHorizontalLocation()
    {
    	return potionLocX;
    }
    
    /**
     * Sets the horizontal location where the potion timers are rendered.
     * @param x
     * @return the new x location
     */
    public static int SetHorizontalLocation(int x)
    {
    	if(x < 0)
    		x = 0;
    	else if(x > mc.displayWidth)
    		x = mc.displayWidth;
    	
    	potionLocX = x;
    	return potionLocX;
    }
    
    /**
     * Gets the vertical location where the potion timers are rendered.
     * @return
     */
    public static int GetVerticalLocation()
    {
    	return potionLocY;
    }

    /**
     * Sets the vertical location where the potion timers are rendered.
     * @param y
     * @return the new y location
     */
    public static int SetVerticalLocation(int y)
    {
    	if(y < 0)
    		y = 0;
    	else if(y > mc.displayHeight)
    		y = mc.displayHeight;
    	
    	potionLocY = y;
    	return potionLocY;
    }
    
}

package com.github.igotyou.FactoryMod.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;

import com.github.igotyou.FactoryMod.FactoryModPlugin;
import com.github.igotyou.FactoryMod.Factorys.ProductionFactory;
import com.github.igotyou.FactoryMod.interfaces.Factory;
import com.github.igotyou.FactoryMod.interfaces.Manager;
import com.github.igotyou.FactoryMod.properties.ProductionProperties;
import com.github.igotyou.FactoryMod.utility.InteractionResponse;
import com.github.igotyou.FactoryMod.utility.InteractionResponse.InteractionResult;
import com.github.igotyou.FactoryMod.recipes.ProductionRecipe;
import com.github.igotyou.FactoryMod.utility.ItemList;
import com.github.igotyou.FactoryMod.utility.NamedItemStack;
import java.util.Date;

//original file:
/**
* Manager.java
* Purpose: Interface for Manager objects for basic manager functionality
*
* @author MrTwiggy
* @version 0.1 1/08/13
*/
//edited version:
/**
* Manager.java	 
* Purpose: Interface for Manager objects for basic manager functionality
* @author igotyou
*
*/

public class ProductionManager implements Manager
{
	private FactoryModPlugin plugin;
	private List<ProductionFactory> producers;
	private int clock;
	
	public ProductionManager(FactoryModPlugin plugin)
	{
		this.plugin = plugin;
		producers = new ArrayList<ProductionFactory>();
		//Set maintenance clock to 0
		clock=0;
		cullFactories();
		updateFactorys();
	}
	
	public void save(File file) throws IOException 
	{
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
		bufferedWriter.append(String.valueOf(clock));
		bufferedWriter.append("\n");		
		for (ProductionFactory production : producers)
		{
			//order: subFactoryType world recipe1,recipe2 central_x central_y central_z inventory_x inventory_y inventory_z power_x power_y power_z active productionTimer energyTimer current_Recipe_number 
			
			Location centerlocation = production.getCenterLocation();
			Location inventoryLoctation = production.getInventoryLocation();
			Location powerLocation = production.getPowerSourceLocation();
			
			
			
			bufferedWriter.append(production.getSubFactoryType());
			bufferedWriter.append(" ");
			
			List<ProductionRecipe> recipes=production.getRecipes();
			for (int i = 0; i < recipes.size(); i++)
			{
				bufferedWriter.append(String.valueOf(recipes.get(i).getTitle()));
				bufferedWriter.append(",");
			}
			bufferedWriter.append(" ");
			
			bufferedWriter.append(centerlocation.getWorld().getName());
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(centerlocation.getBlockX()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(centerlocation.getBlockY()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(centerlocation.getBlockZ()));
			bufferedWriter.append(" ");
			
			bufferedWriter.append(Integer.toString(inventoryLoctation.getBlockX()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(inventoryLoctation.getBlockY()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(inventoryLoctation.getBlockZ()));
			bufferedWriter.append(" ");
			
			bufferedWriter.append(Integer.toString(powerLocation.getBlockX()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(powerLocation.getBlockY()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(powerLocation.getBlockZ()));
			bufferedWriter.append(" ");
			
			bufferedWriter.append(Boolean.toString(production.getActive()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(production.getProductionTimer()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(production.getEnergyTimer()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(production.getCurrentRecipeNumber()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Double.toString(production.getCurrentMaintenance()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(production.getDateDisrepair()));
			bufferedWriter.append("\n");
		}
		bufferedWriter.flush();
		fileOutputStream.close();
	}

	public void load(File file) throws IOException 
	{
		FileInputStream fileInputStream = new FileInputStream(file);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

		clock=Integer.parseInt(bufferedReader.readLine());
		String line;
		while ((line = bufferedReader.readLine()) != null)
		{
			String parts[] = line.split(" ");
			//order: subFactoryType world recipe1,recipe2 central_x central_y central_z inventory_x inventory_y inventory_z power_x power_y power_z active productionTimer energyTimer current_Recipe_number 
			String subFactoryType = parts[0];
			String recipeNames[] = parts[1].split(",");

			Location centerLocation = new Location(plugin.getServer().getWorld(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
			Location inventoryLocation = new Location(plugin.getServer().getWorld(parts[2]), Integer.parseInt(parts[6]), Integer.parseInt(parts[7]), Integer.parseInt(parts[8]));
			Location powerLocation = new Location(plugin.getServer().getWorld(parts[2]), Integer.parseInt(parts[9]), Integer.parseInt(parts[10]), Integer.parseInt(parts[11]));
			boolean active = Boolean.parseBoolean(parts[12]);
			int productionTimer = Integer.parseInt(parts[13]);
			int energyTimer = Integer.parseInt(parts[14]);
			int currentRecipeNumber = Integer.parseInt(parts[15]);
			double maintenance = Double.parseDouble(parts[16]);
			int dateDisrepair  = 99999999;
			if(parts.length==18)
			{
				dateDisrepair  = Integer.parseInt(parts[17]);
			}
			if(FactoryModPlugin.production_Properties.containsKey(subFactoryType))
			{
				List<ProductionRecipe> recipes=new ArrayList<ProductionRecipe>();
				for(String name:recipeNames)
				{
					if(FactoryModPlugin.productionRecipes.containsKey(name))
					{
						recipes.add(FactoryModPlugin.productionRecipes.get(name));
					}
				}

				ProductionFactory production = new ProductionFactory(centerLocation, inventoryLocation, powerLocation, subFactoryType, active, productionTimer, energyTimer, recipes, currentRecipeNumber, maintenance,dateDisrepair);
				addFactory(production);
			}
		}
		fileInputStream.close();
	}

	public void updateFactorys() 
	{
		plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
		{
			@Override
			public void run()
			{
				clock++;
				if(clock>=FactoryModPlugin.MAINTENANCE_CYCLE)
				{
					//Count how many of each recipe there are
					//Reset counters on all recipe object
					for(ProductionRecipe recipe:FactoryModPlugin.productionRecipes.values())
					{
						recipe.setTotalNumber(0);
					}
					//Count recipes in each factory					
					for (ProductionFactory production: producers)
					{
						for (ProductionRecipe recipe: production.getRecipes())
						{
							recipe.incrementCount();
						}
					}
					//Conduct Maintenance on each factory
					for (ProductionFactory production: producers)
					{
						production.degrade(FactoryModPlugin.MAINTENANCE_RATE*(FactoryModPlugin.MAINTENANCE_CYCLE/20.0));
					}
					clock=0;
				}
				for (ProductionFactory production: producers)
				{
					production.update();
				}
			}
		}, 0L, FactoryModPlugin.PRODUCER_UPDATE_CYCLE);
	}

	public InteractionResponse createFactory(Location factoryLocation, Location inventoryLocation, Location powerSourceLocation) 
	{
		if (!factoryExistsAt(factoryLocation))
		{
			HashMap<String, ProductionProperties> properties = plugin.production_Properties;
			Block inventoryBlock = inventoryLocation.getBlock();
			Chest chest = (Chest) inventoryBlock.getState();
			Inventory chestInventory = chest.getInventory();
			String subFactoryType = null;
			for (Map.Entry<String, ProductionProperties> entry : properties.entrySet())
			{
				ItemList<NamedItemStack> inputs = entry.getValue().getInputs();
				if(inputs.exactlyIn(chestInventory))
				{
					subFactoryType = entry.getKey();
				}
			}
			if (subFactoryType != null)
			{
				ProductionFactory production = new ProductionFactory(factoryLocation, inventoryLocation, powerSourceLocation,subFactoryType);
				if (properties.get(subFactoryType).getInputs().allIn(production.getInventory()))
				{
					addFactory(production);
					properties.get(subFactoryType).getInputs().removeFrom(production.getInventory());
					return new InteractionResponse(InteractionResult.SUCCESS, "Successfully created " + production.getProductionFactoryProperties().getName());
				}
			}
			return new InteractionResponse(InteractionResult.FAILURE, "Incorrect materials in chest! Stacks must match perfectly.");
		}
		return new InteractionResponse(InteractionResult.FAILURE, "There is already a factory there!");
	}
	
	public InteractionResponse createFactory(Location factoryLocation, Location inventoryLocation, Location powerSourceLocation, int productionTimer, int energyTimer) 
	{
		if (!factoryExistsAt(factoryLocation))
		{
			HashMap<String, ProductionProperties> properties = plugin.production_Properties;
			Block inventoryBlock = inventoryLocation.getBlock();
			Chest chest = (Chest) inventoryBlock.getState();
			Inventory chestInventory = chest.getInventory();
			String subFactoryType = null;
			boolean hasMaterials = true;
			for (Map.Entry<String, ProductionProperties> entry : properties.entrySet())
			{
				ItemList<NamedItemStack> inputs = entry.getValue().getInputs();
				if(!inputs.allIn(chestInventory))
				{
					hasMaterials = false;
				}
				if (hasMaterials == true)
				{
					subFactoryType = entry.getKey();
				}
			}
			if (hasMaterials && subFactoryType != null)
			{
				ProductionFactory production = new ProductionFactory(factoryLocation, inventoryLocation, powerSourceLocation,subFactoryType);
				if (properties.get(subFactoryType).getInputs().removeFrom(production.getInventory()))
				{
					addFactory(production);
					return new InteractionResponse(InteractionResult.SUCCESS, "Successfully created " + subFactoryType + " production factory");
				}
			}
			return new InteractionResponse(InteractionResult.FAILURE, "Not enough materials in chest!");
		}
		return new InteractionResponse(InteractionResult.FAILURE, "There is already a factory there!");
	}

	public InteractionResponse addFactory(Factory factory) 
	{
		ProductionFactory production = (ProductionFactory) factory;
		if (production.getCenterLocation().getBlock().getType().equals(Material.WORKBENCH) && (!factoryExistsAt(production.getCenterLocation()))
				|| !factoryExistsAt(production.getInventoryLocation()) || !factoryExistsAt(production.getPowerSourceLocation()))
		{
			producers.add(production);
			return new InteractionResponse(InteractionResult.SUCCESS, "");
		}
		else
		{
			return new InteractionResponse(InteractionResult.FAILURE, "");
		}
	}

	public ProductionFactory getFactory(Location factoryLocation) 
	{
		for (ProductionFactory production : producers)
		{
			if (production.getCenterLocation().equals(factoryLocation) || production.getInventoryLocation().equals(factoryLocation)
					|| production.getPowerSourceLocation().equals(factoryLocation))
				return production;
		}
		return null;
	}
	
	public boolean factoryExistsAt(Location factoryLocation) 
	{
		boolean returnValue = false;
		if (getFactory(factoryLocation) != null)
		{
			returnValue = true;
		}
		return returnValue;
	}
	
	public boolean factoryWholeAt(Location factoryLocation) 
	{
		boolean returnValue = false;
		if (getFactory(factoryLocation) != null)
		{
			returnValue = getFactory(factoryLocation).isWhole();
		}
		return returnValue;
	}

	public void removeFactory(Factory factory) 
	{
		producers.remove((ProductionFactory)factory);
	}
	public void cullFactories()
	{
		int currentDate=Integer.valueOf(FactoryModPlugin.dateFormat.format(new Date()));
		for (ProductionFactory production: producers)
		{
			if(currentDate<(production.getDateDisrepair()+FactoryModPlugin.DISREPAIR_LENGTH))
			{
				removeFactory(production);
			}
		} 
	}

	public String getSavesFileName() 
	{
		return FactoryModPlugin.PRODUCTION_SAVES_FILE;
	}

}

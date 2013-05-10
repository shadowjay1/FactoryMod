from ConfigObjects import Recipe, Enchantment, ItemStack, Factory, defaults
import ConfigObjects
import pydot

class ParseConfig:
    @staticmethod
    def prettyList(config,filename='prettylist.txt'):
        myfile=open(filename,'w')
        myfile.write('\n\n##Factory List')
        sortedFactoryKeys=config['factories'].keys()
        sortedFactoryKeys.sort()
        for type,name in [('Enchanting','Cauldron'),('Smelting','Smelter'),('Food','Bakery'),('Equipment','Smithy'),('Wool','Wool')]:
            myfile.write('\n\n###'+type)
            for key in sortedFactoryKeys:
                factory=config['factories'][key]
                if name in factory.name:
                    myfile.write('\n\n**'+factory.name+'**')
                    for input in factory.inputs:
                        myfile.write(' - '+str(input.amount)+' '+input.name)
                    for recipe in factory.outputRecipes:
                        myfile.write('\n\n\t')
                        for output in recipe.outputs:
                            myfile.write(str(output.amount)+' '+output.name)
                        myfile.write(' for ')
                        for input in recipe.inputs:
                            myfile.write(str(input.amount)+' '+input.name)
                            if(recipe.inputs.index(input)!=len(recipe.inputs)-1):
                                myfile.write(', ')
        
    @staticmethod
    def parseItems(itemsConfig):
        items=[]
        for itemConfig in itemsConfig.items():
            name=itemConfig[0]
            info=itemConfig[1]
            if info != None and 'material' in info:
                material=info['material']
                amount=info['amount'] if 'amount' in info else defaults['amount']
                durability=info['durability'] if 'durability' in info else defaults['durability']
                displayName=info['display_name'] if 'display_name' in info else defaults['displayName']
                lore=info['lore'] if 'lore' in info else defaults['lore']
                items.append(ItemStack(material=material,name=name,amount=amount,durability=durability,displayName=displayName,lore=lore))
        return items
    @staticmethod
    def saveConfig(config,filename='config.yml'):
        from shutil import copyfile
        copyfile('template.yml',filename)
        myfile=open(filename,'a')
        myfile.write('copy_defaults: '+config['copy_defaults'])
        myfile.write('\ngeneral:')
        myfile.write('\n  central_block: '+config['central_block'])
        myfile.write('\n  save_cycle: '+config['save_cycle'])
        myfile.write('\n  return_build_materials: '+config['return_build_materials'])
        myfile.write('\n  citadel_enabled: '+config['citadel_enabled'])
        myfile.write('\n  factory_interaction_material: '+config['factory_interaction_material'])
        myfile.write('\n  disable_experience: '+config['disable_experience'])
        myfile.write('\n  disrepair_length: '+config['disrepair_length'])
        myfile.write('\nproduction_general:')
        myfile.write('\n  update_cycle: '+config['update_cycle'])
        myfile.write('\n  maintenance_cycle: '+config['maintenance_cycle'])
        myfile.write('\n  maintenance_rate: '+config['maintenance_rate'])
        myfile.write('\ndisabled_recipes:')
        for disabled_recipe in config['disabled_recipes']:
            myfile.write('\n  - '+disabled_recipe)
        myfile.write('\nproduction_factories:')
        sortedFactoryKeys=config['factories'].keys()
        sortedFactoryKeys.sort()
        for key in sortedFactoryKeys:
            myfile.write(config['factories'][key].cOutput())
        myfile.write('\nproduction_recipes:')
        for recipe in config['recipes'].values():
            myfile.write(recipe.cOutput())
        myfile.close()
    @staticmethod
    def parseConfig(filename):
        import yaml
        file=open(filename,'r')
        yaml=yaml.load(file)
        
        #Parse Recipes
        recipes={}
        outputRecipes={}
        for recipe in yaml['production_recipes'].items():
            identifier=recipe[0]
            entry=recipe[1]
            name=entry['name']
            time=entry['production_time'] if 'production_time' in entry else deafultTime
            inputs=ParseConfig.parseItems(entry['inputs']) if 'inputs' in entry else []
            upgrades=ParseConfig.parseItems(entry['upgrades']) if 'upgrades' in entry else []
            outputs=ParseConfig.parseItems(entry['outputs']) if 'outputs' in entry else []
            enchants=[]
            if 'enchantments' in entry:
                for enchant in entry['enchantments'].items():
                    name=str(enchant[0])
                    info=enchant[1]
                    type=str(info['type'])
                    level=int(info['level']) if 'level' in info else deafults['level']
                    probability=float(info['probability']) if 'probability' in info else defaults['probability']
                    enchants.append(Enchant(type,name=name,level=level,probability=probability))
            useOnce=bool(entry['use_once']) if 'use_once' in entry else defaults['useOnce']
            maintenance=entry['maintenance'] if 'maintenance' in entry else defaults['maintenance']
            recipes[identifier]=Recipe(identifier,name,time=time,inputs=inputs,upgrades=upgrades,outputs=outputs,enchants=enchants,useOnce=useOnce,maintenance=maintenance)
            outputRecipes[recipes[identifier]]=[]
            if 'output_recipes' in entry:
                for outputRecipe in entry['output_recipes']:
                    outputRecipes[recipes[identifier]].append(outputRecipe)
        for recipe in recipes.values():
            for outputRecipe in outputRecipes[recipe]:
                recipe.addOutputRecipe(recipes[outputRecipe])
        
        #Parse factories
        factories={}
        for factory in yaml['production_factories'].items():
            identifier=factory[0]
            entry=factory[1]
            name=entry['name'] if 'name' in entry else defaults['defaultName']
            fuel=next(iter(ParseConfig.parseItems(entry['fuel'])), None) if 'fuel' in entry else defaults['fuel']
            fuelTime=entry['fuel_time'] if 'fuel_time' in entry else defaults['fuelTime']
            inputs=ParseConfig.parseItems(entry['inputs']) if 'inputs' in entry else []
            factoryRecipes=[]
            if 'production_recipes' in entry:
                for productionRecipe in entry['production_recipes']:
                    factoryRecipes.append(productionRecipe)
            maintenanceInput=next(iter(parseItems(entry['maintenance_material']).pop()), None) if 'maintenance_material' in entry else defaults['maintenanceInput']
            factories[identifier]=(Factory(name,identifier,fuel=fuel,fuelTime=fuelTime,inputs=inputs,outputRecipes=outputRecipes,maintenanceInput=maintenanceInput))
        return {'recipes':recipes,'factories':factories}
    @staticmethod
    def saveGraph(config,filename='graph.gv'):
        graph=pydot.Graph("Main")
        graph.set('rankdir','LR')
        graph.add_subgraph(ParseConfig.createGraph(config))
        file=open(filename,'w')
        file.write(graph.to_string()) 
    @staticmethod
    def createGraph(config,name='DefaultName'):
        graph=pydot.Subgraph(name)
        graph.set('rankdir','LR')
        #graph.set('splines','polyline')
        for recipe in config['recipes'].values():
            node=recipe.getNode()
            graph.add_node(node)
            node.set('shape','box')
            node.set('color','slategray')
            node.set('fontname','Courier-Bold')
            node.set('style','bold')
            if len(recipe.outputRecipes)==0:
                node.set('style','filled') 
            edges=recipe.getEdges()
            for edge in edges:
                graph.add_edge(edge)
        for factory in config['factories'].values():
            node=factory.getNode()
            graph.add_node(node)
            node.set('shape','tripleoctagon')
            node.set('color','slategray')
            if len(recipe.outputRecipes)==0:
                node.set('style','filled') 
            edges=factory.getEdges()
            for edge in edges:
                graph.add_edge(edge)
        return graph
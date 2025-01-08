package org.rusherhack.bonemeal;

import org.rusherhack.client.api.RusherHackAPI;

/**
 * Example rusherhack plugin
 *
 * @author John200410
 */
public class Plugin extends org.rusherhack.client.api.plugin.Plugin {
	
	@Override
	public void onLoad() {
		
		//logger
		this.getLogger().info("Loading AutoBoneMeal");
		
		//register module
		RusherHackAPI.getModuleManager().registerFeature(new AutoBoneMealModule());
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("AutoBoneMeal unloaded!");
	}
	
}
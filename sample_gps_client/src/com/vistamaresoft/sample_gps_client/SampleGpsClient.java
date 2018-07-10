/****************************
	S a m p l e G p s C l i e n t  -  A Java plug-in for Rising World exemplifying
										how to send commands to the GPS plug-in.

	SampleGpsClient.java - The main plug-in class

	Created by : Maurizio M. Gavioli 2018-06-24

	(C) Maurizio M. Gavioli (a.k.a. Miwarre), 2018
	Licensed under the Creative Commons by-sa 3.0 license (see http://creativecommons.org/licenses/by-sa/3.0/ for details)

*****************************/

package com.vistamaresoft.sample_gps_client;

// import from the Cps class of the GPS plug-in:
import com.vistamaresoft.gps.Gps;
// other imports, as needed
import net.risingworld.api.Plugin;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

/**
 *	Main (and only) sample-plug-in class
 */
public class SampleGpsClient extends Plugin implements Listener
{
	// the player chat commands this plug-in knows (currently just one):
	public static final		String	targetCommand	= "/gt";
	// where to store the Gps plug-in, once we find it:
	static					Gps		gpsPlugin		= null;

	//------------------
	// E V E N T S
	//------------------

	@Override
	public void onEnable()
	{
		// retrieve GPS plug-in, if present and check it is 'our' GPS;
		// the GPS plug-in name ("com.vms.GPS") comes from the "name"
		// line of the plug-in resources/plugin.yml file.
		Gps	tempGpsPlugin;
		if ( (tempGpsPlugin = (Gps)getPluginByName("com.vms.GPS")) != null
				&& tempGpsPlugin instanceof Gps)
		// if OK, store it in the global variable
			gpsPlugin		= tempGpsPlugin;
		registerEventListener(this);
		System.out.println("Sample GPS Client enabled successfully!");
	}

	@Override
	public void onDisable()
	{
		unregisterEventListener(this);
		System.out.println("Sample GPS Client disabled successfully!");
	}
	
	/**	Called when the player issues a command ("/...") in the chat window
	
		@param event	the command event
	*/
	@EventMethod
	public void onPlayerCommand(PlayerCommandEvent event)
	{
		Player		player	= event.getPlayer();
		String[]	cmd		= event.getCommand().split(" ");

		if (cmd[0].equals(targetCommand) )
		{
			// This rather useless command generates a new GPS target within 100 m
			// of the current player position and sends it to the GPS as a target.
			// Get player position
			if (gpsPlugin != null)
			{
				Vector3f	playerPos	= player.getPosition();
				// Generate a position within 100 m from the player
				float		x			= (float) (playerPos.x + (Math.random()*400) - 200);
				float		z			= (float) (playerPos.z + (Math.random()*400) - 200);
				// send it to the GPS
				gpsPlugin.addTarget(player, "Auto target", x, playerPos.y, z);
				// just for feedback, notify the player
				player.sendTextMessage("New target generated at "+(int)(z)+","+(int)(x));
			}
		}
	}

}

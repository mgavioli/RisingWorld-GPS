/****************************
	G P S  -  A Java plug-in for Rising World.

	Gps.java - The main plug-in class

	Created by : Maurizio M. Gavioli 2016-08-15

	(C) Maurizio M. Gavioli (a.k.a. Miwarre), 2016
	Licensed under the Creative Commons by-sa 3.0 license (see http://creativecommons.org/licenses/by-sa/3.0/ for details)

*****************************/

package com.vistamaresoft.gps;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

//import com.vistamaresoft.televator.Db;
//import com.vistamaresoft.televator.Msgs;

import net.risingworld.api.Plugin;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChangePositionEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
//import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.gui.Font;
import net.risingworld.api.gui.GuiLabel;
import net.risingworld.api.gui.PivotPosition;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

public class Gps extends Plugin implements Listener
{
	// Settings with their default values
	static final public boolean	allowTpToWpDef		= false;
	static final public int		wpNameDispLenDef	= 8;
	static final public int		wpHdgPrecisDef		= 5;
	static public		String	commandPrefix		= "/gps";
	static public		boolean	allowTpToWp			= allowTpToWpDef;	// whether teleporting to waypoints (in addition to home) is possible or not
	static public		int		wpDispLen			= wpNameDispLenDef;	// the max length of waypoint names to display on screen
	static public		int		wpHdgPrecis			= wpHdgPrecisDef;	// the waypoint radial delta below which route corrections arrows are not displayed

	// attribute keys
	static final public String	key_gpsShow			= "gpsShow";
	static final public String	key_gpsHomeShow		= "gpsHomeGUI";
	static final public String	key_gpsLabel		= "gpsLabel";
	static final public String	key_gpsWpList		= "gpsWpList";
	static final public String	key_gpsWpShow		= "gpsWpGUI";

	// Constants
	static final public	double	rad2deg		= 180.0 / Math.PI;
	static final public double	version		= 0.1;
	static final public int		homeWp		= 0;			// the index of the home waypoint
	static final public int		maxWp		= 9;			// the max waypoint index
	static final public int		minWp		= 0;			// the min waypoint index (including home)
	static final public int		minWpProper	= 1;			// the min waypoint index (EXCLUDING home)

	//------------------
	// E V E N T S
	//------------------

	// onEnable() / onDisable()
	//
	// called at script loading/unloading. Required for the script to work.
	 
	@Override
	public void onEnable()
	{
		initSettings();
		Db.init(this);							// init DB, if required
		System.out.println(Msgs.msg_init);
		registerEventListener(this);
	}

	@Override
	public void onDisable()
	{
		unregisterEventListener(this);
		Db.deinit();
		System.out.println(Msgs.msg_deinit);
	}

	// onPlayerConnect
	//
	// called when the player connects to the server (but has not entered the world yet)
	@EventMethod
	public void onPlayerSpawn(PlayerSpawnEvent event)
	{
		Player		player	= event.getPlayer();
		GuiLabel	info	= new GuiLabel("", 0.5f, 0.15f, true);
		info.setColor(0x0000007f);
		info.setFont(Font.DefaultMono);
		info.setFontColor(0xFFFFFFFF);
		info.setFontSize(18);
		info.setPivot(PivotPosition.Center);

		player.addGuiElement(info);
		player.setAttribute(key_gpsLabel, info);		// the textual label with GPS data
		player.setAttribute(key_gpsShow, true);			// whether the GPS text is shown or not
		player.setAttribute(key_gpsHomeShow, false);	// whether the home info is shown or not
		player.setAttribute(key_gpsWpShow, 0);			// which waypoint is shown, if any (0 = none)

		Db.loadPlayer(player);
		setGpsText(player);								// set initially displayed text
	}

	// onPlayerCommand
	//
	// called when the player issues a command ("/...") in the chat window
	// Toggles the GPS info display on the "/gps" command
	@EventMethod
	public void onPlayerCommand(PlayerCommandEvent event)
	{
		Player		player	= event.getPlayer();
		String[]	cmd		= event.getCommand().split(" ");

		if (cmd[0].equals(commandPrefix) )
		{
			switch(cmd.length)
			{
			case 1:
				// if no sub-command, flip text display on/off
				setGPSShow(player, !(boolean)player.getAttribute(key_gpsShow));
				return;
			case 2:
				switch (cmd[1])
				{
				case "on":
					setGPSShow(player, true);
					return;
				case "off":
					setGPSShow(player, false);
					return;
				case "help":
					help(player);
					return;
				case "showhome":
					player.setAttribute(key_gpsHomeShow, !(boolean)player.getAttribute(key_gpsHomeShow) );
					setGpsText(player);				// update displayed text
					return;
				case "list":
					listWp(player);
					return;
				case "sethome":
					Db.setHome(player);
					return;
				case "home":
					teleportToWp(player, homeWp);
					return;
				}
				break;
			case 3:
				switch (cmd[1])
				{
				case "showwp":
					setShowWp(player, toInteger(cmd[2]));
					return;
				case "goto":
					teleportToWp(player, toInteger(cmd[2]));
					return;
				}
				break;
			}

			// "/gps setwp" has a variable number of parameters
			if (cmd[1].equals("setwp") )
			{
				setWp(player, cmd.length > 2 ? cmd[2] : null, cmd.length > 3 ? cmd[3] : null);
				return;
			}

			// if the command was not processed so far => error
			player.sendTextMessage(Msgs.err_invalidCmd + event.getCommand() + "'");
		}

		// 2 commands added for compatibility with other common scripts
		if (cmd[0].equals("/sethome") )
			Db.setHome(player);

		if (cmd[0].equals("/home") )
			teleportToWp(player, homeWp);
	}

	// onPlayerChangePosition
	//
	// called when the player changes of world position.
	// Note: change of view direction without any actual displacement
	// does not always trigger this event, particularly while flying
	@EventMethod
	public void onPlayerChangePosition(PlayerChangePositionEvent event)
	{
		setGpsText(event.getPlayer());
	}

	//------------------
	// U T I L I T Y  F U N C T I O N S
	//------------------

	// setGpsText(player)
	//
	// actually fills the player GPS info text

	protected void setGpsText(Player player)
	{
		GuiLabel labelgpsInfo = (GuiLabel) player.getAttribute(key_gpsLabel);
		if (labelgpsInfo == null)
			return;

		if ((boolean) player.getAttribute(key_gpsShow) )
		{
			// PLAYER ROTATION
			Vector3f	playerRot	= player.getViewDirection();
			// x- and z-components of the viewing vector
			double		rotX		= playerRot.x;
			double		rotZ		= playerRot.z;
			// if the viewing vector is not horizontal but slanted up or down,
			// the x and z components are shortened: scale them up until
			// they are the catheti of a triangle whose hypotenuse is long 1
			double	scale			= Math.sqrt(rotX*rotX + rotZ*rotZ);
			double heading;
			if (scale < 0.00001)				// avoid division by 0
				heading				= 0.0;
			else
			// get the heading angle and convert to degrees
				heading				= Math.acos(rotZ / scale) * rad2deg;
			// hdg is correct from N (0°) through E (90°) to S (180°)
			// then, when view is toward W, it decreases down to 0° again;
			// when view is toward W, correct by taking the complementary angle
			if (rotX > 0)			heading = 360 - heading;
			// round to nearest integer and uniform 0° to 360°
			int		hdg				= (int)Math.floor(heading + 0.5);
			if (hdg == 0)			hdg = 360;

			// PLAYER POSITION
			Vector3f	playerPos = player.getPosition();
			int			posE		= (int) Math.floor(-playerPos.x);	// convert positive W to standard, positive E
			int			posN		= (int) Math.floor(playerPos.z);
			int			posH		= (int) Math.floor(playerPos.y);
			// set N/S and E/W according to signs of coordinates
			String		latDir		= "N,";
			if (posN < 0)
			{
				posN	= -posN;
				latDir	= "S,";
			}
			String		longDir		= "E) h";
			if (posE < 0)
			{
				posE	= -posE;
				longDir	= "W) h";
			}
			// OUTPUT: home
			String		text		= "";
			Waypoint[]	wps			= (Waypoint[])player.getAttribute(key_gpsWpList);
			Waypoint	home;
			if ((boolean)player.getAttribute(key_gpsHomeShow) && wps != null && (home=wps[homeWp]) != null)
				text = home.toString(heading, playerPos) + " | ";
			// main data
			text	+= String.format("%03d°", hdg) + " (" + posN + latDir + posE + longDir + posH;
			// waypoint
			int			wpToShow	= (int) player.getAttribute(key_gpsWpShow);
			if (wpToShow > 0)
			{
				Waypoint	wp		= ((Waypoint[])player.getAttribute(key_gpsWpList))[wpToShow];
				if (wp != null)
					text += " | " + wp.toString(heading, playerPos);
			}

			labelgpsInfo.setText(text);
		}
		else
			labelgpsInfo.setText("");
	}

	// setGPSShow(player, show)
	//
	// sets whether the GPS text is shown or not for player

	protected void setGPSShow(Player player, boolean show)
	{
		player.setAttribute(key_gpsShow, show);
		setGpsText(player);							// update displayed text
	}

	// listWp(player)
	//
	// Lists all defined waypoints (including home) for player

	protected void listWp(Player player)
	{
		Waypoint[]	waypoints = (Waypoint[]) player.getAttribute(key_gpsWpList);
		player.sendTextMessage(Msgs.msg_wpList);
		for (int i=minWp; i<= maxWp; i++)
		{
			Waypoint	wp	= waypoints[i];
			String		txt;
			if (wp != null && (txt = wp.toString()) != null)
				player.sendTextMessage(txt);
		}
	}

	// setWp(player, par1, par2)
	//
	// Sets a new waypoint for player according to par1 and par2.
	// par1 and par2 contain the index and the name of the waypoint to create, in that order;
	// either or both can be null, in which case an index and/or a name are supplied by the function;
	// but if both are present they cannot be both indices or both names.
	// If an index is not given, the function reuses the first available slot, if any, or overwrite
	// waypoint 1 if no slot is available

	protected void setWp(Player player, String par1, String par2)
	{
		Integer	idx		= 0;							// prepare empty waypoint index and name
		String	name	= "";

		if (par2 != null)								// par2 must be a name, if present
			name	= par2;
		
		if (par1 != null)								// if par1 is present...
		{
			idx = toInteger(par1);						// check it is an int
			if (idx	== null)							// if it is not an int (=> a name)...
			{
				if (name.length() > 0)					// ...and we already have a name => error
				{
					player.sendTextMessage(Msgs.err_setWpDuplName);
					return;
				}
				name	= par1;							// use par1 as name
			}
		}												// otherwise, keep par1 as an idx

		// if no idx, look for a suitable index
		if (idx == null || idx == 0)
		{
			for (int i = minWpProper; i <= maxWp; i++)
			{
				Waypoint	wp	= ((Waypoint[])player.getAttribute(key_gpsWpList))[i];
				if (wp == null || wp.name == null || wp.name == "")
				{
					idx = i;
					break;
				}
			}
		}
		if (idx == null || idx == 0)	idx = minWpProper;	// if still no index, use first index
		// if idx out of range => error
		if (idx < 0 || idx > maxWp)
		{
			player.sendTextMessage(Msgs.err_showWpInvalidIndex);
			return;
		}
		// if no name, create a name from wp index
		if (name == "")
			name = String.format("wp%d", idx);

		Db.setWp(player, idx, name);
	}

	// setShowWp(player, index)
	//
	// turn on/off display of a waypoint data. Index = 0 => turn off waypoint data display

	protected void setShowWp(Player player, Integer index)
	{
		// check index is there and is legal
		if (index == null || index < minWpProper || index > maxWp)
		{
			player.sendTextMessage(Msgs.err_showWpInvalidIndex);
			return;
		}
		// if not turning off (index = 0), check that waypoint exists
		if (index > 0 && ((Waypoint[])player.getAttribute(key_gpsWpList))[index] == null)
		{
			player.sendTextMessage(String.format(Msgs.err_showWpUndefinedWp, index));
			return;
		}
		player.setAttribute(key_gpsWpShow, index);
		setGpsText(player);						// update displayed text
	}

	// teleportToWp(player, index)
	//
	// teleports to the index-th waypoint (incl. home)

	protected void teleportToWp(Player player, Integer index)
	{
		// check index is there and is legal
		if (index == null || index < minWp || index > maxWp)
		{
			player.sendTextMessage(Msgs.err_showWpInvalidIndex);
			return;
		}
		// check teleporting to waypoint is enabled
		if (index > 0 && !allowTpToWp)
		{
			player.sendTextMessage(Msgs.err_noTpToWp);
			return;
		}
		// check that waypoint exists
		Waypoint wp = ((Waypoint[])player.getAttribute(key_gpsWpList))[index];
		if (wp == null)
		{
			player.sendTextMessage(String.format(Msgs.err_showWpUndefinedWp, index));
			return;
		}
		player.setPosition(wp.pos.x, wp.pos.y, wp.pos.z);
		setGpsText(player);						// update displayed text
	}

	// help()
	//
	// Displays help summary

	protected void help(Player player)
	{
		for (String txt : Msgs.txt_help)
			player.sendTextMessage(txt);
	}

	// toInteger(val)
	//
	// returns txt as an integer number if it can be interpreted as one or 0 if it cannot.
	 
	public Integer toInteger(String txt)
	{
		if (txt == null)
			return 0;
		Integer val;
		try {
			val = Integer.parseInt(txt);
		} catch (NumberFormatException e) {		// txt cannot be parsed as a number
			return 0;
		}
		return val;
	}

	// initSettings()
	//
	// initialises settings from settings file.

	protected void initSettings()
	{
		// create and load settings from disk
		Properties settings	= new Properties();
		// NOTE : use getResourcesAsStream() if the setting file is included in the distrib. .jar)
		FileInputStream in;
		try {
			in = new FileInputStream(getPath() + "/settings.properties");
			settings.load(in);
			in.close();
			// fill global values
			commandPrefix	= "/" + settings.getProperty("command", commandPrefix);
			allowTpToWp		= Integer.parseInt(settings.getProperty("allowTpToWp", allowTpToWp ? "1" : "0")) != 0;
			wpDispLen		= toInteger(settings.getProperty("wpDispLength", Integer.toString(wpDispLen)));
			wpHdgPrecis		= toInteger(settings.getProperty("wpHdgPrecis", Integer.toString(wpHdgPrecis)));
		} catch (IOException e) {
//			e.printStackTrace();
			return;					// settings are init'ed anyway: on exception, do nothing
		}
	}
}

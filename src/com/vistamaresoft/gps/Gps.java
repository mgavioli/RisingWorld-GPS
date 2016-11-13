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
import java.util.Locale;
import java.util.Properties;
import net.risingworld.api.Plugin;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChangePositionEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.events.player.gui.PlayerGuiElementClickEvent;
import net.risingworld.api.events.player.gui.PlayerGuiInputEvent;
import net.risingworld.api.gui.Font;
import net.risingworld.api.gui.GuiLabel;
import net.risingworld.api.gui.PivotPosition;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

public class Gps extends Plugin implements Listener
{
	// SETTINGS with their default values
	public static final boolean	allowTpToWpDef		= false;
	public static final float	gpsXPosDef			= 0.5f;
	public static final float	gpsYPosDef			= 0.1f;
	public static final float	gpsHintYPos			= -0.25f;
	public static final String	localeLanguageDef	= "en";
	public static final int		wpNameDispLenDef	= 8;
	public static final int		wpHdgPrecisDef		= 5;

	public static		String	commandPrefix		= "/gps";
	public static		boolean	allowTpToWp			= allowTpToWpDef;	// whether teleporting to waypoints (in addition to home) is possible or not
	public static		float	gpsYPos				= gpsYPosDef;
	public static		Locale	locale;
	public static		int		wpDispLen			= wpNameDispLenDef;	// the max length of waypoint names to display on screen
	public static		int		wpHdgPrecis			= wpHdgPrecisDef;	// the waypoint radial delta below which route corrections arrows are not displayed

	// KEYS FOR PLAYER ATTRIBUTES
	public static final String	key_gpsShow			= "com.vms.gpsShow";
	public static final String	key_gpsGUI			= "com.vms.gpsGUI";
	public static final String	key_gpsHomeShow		= "com.vms.gpsHomeShow";
	public static final String	key_gpsLabel		= "com.vms.gpsLabel";
	public static final String	key_gpsHint			= "com.vms.gpsHint";
	public static final String	key_gpsWpList		= "com.vms.gpsWpList";
	public static final String	key_gpsWpShow		= "com.vms.gpsWpShow";

	// CONSTANTS
	public static final double	RAD2DEG			= 180.0 / Math.PI;
	public static final String	VERSION			= "1.0.0";
	public static final int		HOME_WP			= 0;			// the index of the home waypoint
	public static final int		MAX_WP			= 15;			// the max waypoint index
	public static final int		MIN_WP			= 0;			// the min waypoint index (including home)
	public static final int		MIN_WP_PROPER	= 1;			// the min waypoint index (EXCLUDING home)
	public static final int		FONT_SIZE		= 18;			// the size of the info window font
	public static final int		HINT_SIZE		= 13;			// the size of the info window font

	//------------------
	// E V E N T S
	//------------------

	/** Called at plug-in loading. Required for the plug-in to work. */
	@Override
	public void onEnable()
	{
		initSettings();
		Msgs.init(getPath(), locale);
		Db.init(this);							// init DB, if required
		System.out.println(Msgs.msg[Msgs.msg_init]);
		registerEventListener(this);
	}

	/** Called at plug-in unloading. Releases all resources. */
	@Override
	public void onDisable()
	{
		unregisterEventListener(this);
		Db.deinit();
		System.out.println(Msgs.msg[Msgs.msg_deinit]);
	}

	/**
		Called by Rising World when the player connects to a world and he spawns into it.

		@param event	the connect event
	*/
	@EventMethod
	public void onPlayerConnect(PlayerConnectEvent event)
	{
		initPlayer(event.getPlayer());
	}

	/** Called by Rising World when the player spawns into a world after having connected.
	
		@param	event	the spawn event
	*/
	@EventMethod
	public void onPlayerSpawn(PlayerSpawnEvent event)
	{
		setGpsText(event.getPlayer());
	}

	/**	Called when the player issues a command ("/...") in the chat window
	
		@param event	the command event
	*/
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
				// if no sub-command, show the GPS GUI panel
				GpsGUI	gui;
				if (!player.hasAttribute(key_gpsGUI))
					gui	= createGpsGUI(player);
				else
					gui	= (GpsGUI)player.getAttribute(key_gpsGUI);
				if (gui != null)
					gui.show(player);
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
					setShowHome(player);
					return;
				case "list":
					listWp(player);
					return;
				case "sethome":
					Db.setHome(player);
					return;
				case "home":
					teleportToWp(player, HOME_WP);
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
			teleportToWp(player, HOME_WP);

	}

	/**	Called when the player changes of world position.
		Note: change of view direction without any actual displacement
		does not always trigger this event, particularly while flying.
	*/
	@EventMethod
	public void onPlayerChangePosition(PlayerChangePositionEvent event)
	{
		setGpsText(event.getPlayer());
	}

	/**	Called when the player clicks on the GUI

		@param	player	the player who clicked the GUI
	*/
	@EventMethod
	public void onPlayerClick(PlayerGuiElementClickEvent event)
	{
		Player	player	= event.getPlayer();
		GpsGUI	gui		= (GpsGUI)player.getAttribute(key_gpsGUI);
		if (gui != null)
			gui.click(event.getGuiElement(), player);
	}

	@EventMethod
	public void onTextEntry(PlayerGuiInputEvent event)
	{
		Player	player	= event.getPlayer();
		GpsGUI	gui		= (GpsGUI)player.getAttribute(key_gpsGUI);
		if (gui != null)
			gui.textEntry(event.getGuiElement(), player, event.getInput());
	}

	//------------------
	// STATIC METHODS EXTERNALLY ACCESSIBLE
	//------------------

	/**
		Turns on/off the GPS display.

		@param	player	the player for whom to toggle the GSP display
	 	@param	show	true to show | false to hide
	*/
	static public void setGPSShow(Player player, boolean show)
	{
		player.setAttribute(key_gpsShow, show);
		GuiLabel labelgpsInfo = (GuiLabel) player.getAttribute(key_gpsLabel);
		if (labelgpsInfo != null)
			labelgpsInfo.setVisible(show);
		setGpsText(player);							// update displayed text
	}

	/**
		Toggles on/off the display of home way-point data

		@param	player	the player for which to change the home display
	*/
	static protected void setShowHome(Player player)
	{
		player.setAttribute(key_gpsHomeShow, !(boolean)player.getAttribute(key_gpsHomeShow) );
		setGpsText(player);				// update displayed text
	}

	/**
		Controls the display of way-point data.

		@param	player	the affected player.
	 	@param	index	an integer from 1 to 9 to display the corresponding
	 					way-point or 0 to turn way-point display off
	*/
	static protected void setShowWp(Player player, Integer index)
	{
		// check index is there and is legal
		if (index == null || index < MIN_WP || index > MAX_WP)
		{
			player.sendTextMessage(Msgs.msg[Msgs.err_showWpInvalidIndex]);
			return;
		}
		// if not turning off (index = 0), check that waypoint exists
		if (index > 0 && ((Waypoint[])player.getAttribute(key_gpsWpList))[index] == null)
		{
			player.sendTextMessage(String.format(Msgs.msg[Msgs.err_showWpUndefinedWp], index));
			return;
		}
		player.setAttribute(key_gpsWpShow, index);
		setGpsText(player);						// update displayed text
	}

	/**
		Sets a way-point to the current player position.

		par1 and par2 contain the index and the name of the waypoint to create,
		in that order; either or both can be null, in which case an index
		and/or a name are supplied by the function.
		<p>
		If an index is not given, the function reuses the first available slot, if any, or overwrite
		waypoint 1 if no slot is available

	 	@param	player	the affected player.
		@param	index	the index where to store the new way-point.
		@param	name	the name of the new way-point.
	*/
	static protected void setWp(Player player, String index, String name)
	{
		Integer	idx		= 0;							// prepare empty waypoint index and name
		String	locName	= "";

		if (name != null)								// 'name' must be a name, if present
			locName	= name;

		if (index != null)								// if index is present...
		{
			idx = toInteger(index);						// check it is an int
			if (idx	== null)							// if it is not an int (=> a name)...
			{
				if (locName.length() > 0)				// ...and we already have a name => error
				{
					player.sendTextMessage(Msgs.msg[Msgs.err_setWpDuplName]);
					return;
				}
				locName	= index;						// use par1 as name
			}
		}												// otherwise, keep par1 as an idx

		// if no idx, look for a suitable index
		if (idx == null || idx == 0)
		{
			for (int i = MIN_WP_PROPER; i <= MAX_WP; i++)
			{
				Waypoint	wp	= ((Waypoint[])player.getAttribute(key_gpsWpList))[i];
				if (wp == null || wp.name == null || wp.name == "")
				{
					idx = i;
					break;
				}
			}
		}
		if (idx == null || idx == 0)	idx = MIN_WP_PROPER;	// if still no index, use first index
		// if idx out of range => error
		if (idx < 0 || idx > MAX_WP)
		{
			player.sendTextMessage(Msgs.msg[Msgs.err_showWpInvalidIndex]);
			return;
		}
		// if no name, create a name from wp index
		if (locName == "")
			locName = String.format("wp%d", idx);

		Db.setWp(player, idx, locName);
	}

	/**
		Teleports to the index -th waypoint.
		The way-point must be defined and, if different from Home, teleport to
		way-points must be enabled.

	 	@param	player	the affected player.
	 	@param	index	a int from 0 to 9 with the index of the way-point.
	*/
	static protected void teleportToWp(Player player, Integer index)
	{
		// check index is there and is legal
		if (index == null || index < MIN_WP || index > MAX_WP)
		{
			player.sendTextMessage(Msgs.msg[Msgs.err_showWpInvalidIndex]);
			return;
		}
		// check teleporting to waypoint is enabled
		if (index > 0 && !allowTpToWp)
		{
			player.sendTextMessage(Msgs.msg[Msgs.err_noTpToWp]);
			return;
		}
		// check that waypoint exists
		Waypoint wp = ((Waypoint[])player.getAttribute(key_gpsWpList))[index];
		if (wp == null)
		{
			player.sendTextMessage(String.format(Msgs.msg[Msgs.err_showWpUndefinedWp], index));
			return;
		}
		player.setPosition(wp.pos.x, wp.pos.y, wp.pos.z);
		setGpsText(player);						// update displayed text
	}

	/**
		Sets the text of the player GPS data text.

 		@param	player	the affected player
	*/
	static protected void setGpsText(Player player)
	{
		if (player == null)
			return;
		if (player.getAttribute(key_gpsLabel) == null)
			return;
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
				heading				= Math.acos(rotZ / scale) * RAD2DEG;
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
			if ((boolean)player.getAttribute(key_gpsHomeShow) && wps != null && (home=wps[HOME_WP]) != null)
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

	//------------------
	// U T I L I T Y  F U N C T I O N S
	//------------------

	private void initPlayer(Player player)
	{
		if (player.getAttribute(key_gpsLabel) == null)
		{
			// The main textual GUI element showing the GPS data
			GuiLabel	info	= new GuiLabel("", gpsXPosDef, gpsYPos, true);
			info.setColor(0x0000007f);
			info.setFont(Font.DefaultMono);
			info.setFontColor(0xFFFFFFFF);
			info.setFontSize(FONT_SIZE);
			info.setPivot(PivotPosition.Center);
			GuiLabel	hint	= new GuiLabel(Msgs.msg[Msgs.txt_gpsHelpHint], gpsXPosDef, gpsHintYPos, true);
			hint.setPivot(PivotPosition.Center);
			hint.setFontSize(HINT_SIZE);
			info.addChild(hint);
	
			// player attributes keeping track of status (whether the GPS data are shown or not
			// and what they should contain)
			player.setAttribute(key_gpsShow, true);			// whether the GPS text is shown or not
			player.setAttribute(key_gpsHomeShow, false);	// whether the home info is shown or not
			player.setAttribute(key_gpsWpShow, 0);			// which waypoint is shown, if any (0 = none)
	
			Db.loadPlayer(player);							// load player-dependent data
			player.addGuiElement(info);
			player.addGuiElement(hint);
			player.setAttribute(key_gpsLabel, info);
		}
	}

	private GpsGUI createGpsGUI(Player player)
	{
		// the GPS GUI panel
		GpsGUI	gui		= new GpsGUI(this, player, gpsYPos);
		player.addGuiElement(gui);
		player.setAttribute(key_gpsGUI, gui);
		return gui;
	}

	/**
		Lists in the chat the defined way-points.

	 	@param	player	the affected player
	*/
	public void listWp(Player player)
	{
		Waypoint[]	waypoints = (Waypoint[]) player.getAttribute(key_gpsWpList);
		player.sendTextMessage(Msgs.msg[Msgs.msg_wpList]);
		for (int i=MIN_WP; i<= MAX_WP; i++)
		{
			Waypoint	wp	= waypoints[i];
			String		txt;
			if (wp != null && (txt = wp.toString()) != null)
				player.sendTextMessage(txt);
		}
	}

	/**
		Displays in the player chat the help summary text.

		@param	player	the target player
	*/
	public void help(Player player)
	{
		for (int i = Msgs.txt_help_from; i <= Msgs.txt_help_to; i++)
			player.sendTextMessage(Msgs.msg[i]);
	}

	/**
		Returns txt as an integer number if it can be interpreted as one
		or 0 if it cannot.

	 	@param	txt	the String to interpret as an int
	 	@return	the equivalent Integer or null if txt cannot represent an integer.
	 */
	static public Integer toInteger(String txt)
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

	/**
		Initialises settings from settings file.
	*/
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
			gpsYPos			= Float.parseFloat(settings.getProperty("gpsYPos", Float.toString(gpsYPos)));
			wpDispLen		= toInteger(settings.getProperty("wpDispLength", Integer.toString(wpDispLen)));
			wpHdgPrecis		= toInteger(settings.getProperty("wpHdgPrecis", Integer.toString(wpHdgPrecis)));
			// locale is a bit more complex
			String		strLocale		= settings.getProperty("locale", localeLanguageDef);
			String[]	localeParams	= strLocale.split("-");
			if (localeParams.length > 0)
			{
				if (localeParams.length > 1 && localeParams[2].length() > 0)
				{
					if (localeParams.length > 2 && localeParams[2].length() > 0)
						locale	= new Locale(localeParams[0], localeParams[1], localeParams[2]);
					else
						locale	= new Locale(localeParams[0], localeParams[1]);
				}
				else
					locale	= new Locale(localeParams[0]);
			}
			else
				locale	= new Locale(localeLanguageDef);
		} catch (IOException e) {
			e.printStackTrace();
			locale	= new Locale(localeLanguageDef);
			return;					// settings are init'ed anyway: on exception, do nothing
		}
	}
}

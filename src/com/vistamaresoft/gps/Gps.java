/****************************
	G P S  -  A Java plug-in for Rising World for self-locating and world navigation.

	Gps.java - The main plug-in class

	Created by : Maurizio M. Gavioli 2016-08-15

	(C) Maurizio M. Gavioli (a.k.a. Miwarre), 2016
	Licensed under the Creative Commons by-sa 3.0 license (see http://creativecommons.org/licenses/by-sa/3.0/ for details)

*****************************/

package com.vistamaresoft.gps;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import net.risingworld.api.Plugin;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.player.PlayerChangePositionEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.gui.Font;
import net.risingworld.api.gui.GuiLabel;
import net.risingworld.api.gui.PivotPosition;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

public class Gps extends Plugin implements Listener
{
	// SETTINGS with their default values
	static final float	gpsXPosDef			= 0.5f;
	static final float	gpsHintYPos			= -0.25f;
	static final String	localeLanguageDef	= "en";
	static final int	wpNameDispLenDef	= 8;
	static final int	wpHdgPrecisDef		= 5;

	static	String	commandPrefix		= "/gps";
	static	boolean	allowTpToWp			= false;	// whether teleporting to waypoints (in addition to home) is possible or not
	static	boolean	coordNativeFormat	= false;
	static	float	gpsYPos				= 0.1f;
	static	Locale	locale;
	static	int		wpDispLen			= wpNameDispLenDef;	// the max length of waypoint names to display on screen
	static	int		wpHdgPrecis			= wpHdgPrecisDef;	// the waypoint radial delta below which route corrections arrows are not displayed

	// KEYS FOR PLAYER ATTRIBUTES
	static final String	key_gpsShow			= "com.vms.gpsShow";
	static final String	key_gpsGUIcurrWp	= "com.vms.gpsGUIcurrWp";
	static final String	key_gpsHomeShow		= "com.vms.gpsHomeShow";
	static final String	key_gpsLabel			= "com.vms.gpsLabel";
	static final String	key_gpsHint			= "com.vms.gpsHint";
	static final String	key_gpsTargetList	= "com.vms.gpsTargetList";
	static final String	key_gpsWpList		= "com.vms.gpsWpList";
	static final String	key_gpsWpShow		= "com.vms.gpsWpShow";

	// CONSTANTS
	       static final	double	RAD2DEG			= 180.0 / Math.PI;
	       static final	String	VERSION			= "1.4.0";
	       static final	int		VERSION_INT		= 10400;
	       static final	String	publicName		= "GPS";
	public static final	int		HOME_WP			= 0;			// the index of the home waypoint
	public static final	int		MAX_WP			= 15;			// the max waypoint index
	public static final	int		MIN_WP			= 0;			// the min waypoint index (including home)
	public static final	int		MIN_WP_PROPER	= 1;			// the min waypoint index (EXCLUDING home)
	public static final	int		TARGET_ID		= -1;			// the wp ID common to all targets
	       static final	float	TARGET_MIN_DIST	= 9;			// the distance (in blocks) below which a target has been reached
	       static final	int		FONT_SIZE		= 18;			// the size of the info window font
	       static final	int		HINT_SIZE		= 13;			// the size of the info window font

	       static		Gps		plugin;
	//------------------
	// E V E N T S
	//------------------

	/** Called at plug-in loading. Required for the plug-in to work. */
	@Override
	public void onEnable()
	{
		plugin	= this;
		initSettings();
		Msgs.init(getPath(), locale);
		Db.init(this);							// init DB, if required
		System.out.println("GPS "+VERSION+" enabled successfully!");
		registerEventListener(this);
	}

	/** Called at plug-in unloading. Releases all resources. */
	@Override
	public void onDisable()
	{
		unregisterEventListener(this);
		Db.deinit();
		System.out.println("GPS "+VERSION+" disabled successfully!");
	}

	/**
		Called by Rising World when the player connects to a world and he spawns into it.

		@param event	the connect event
	*/
	@EventMethod
	public void onPlayerConnect(PlayerConnectEvent event)
	{
//		System.out.println("GPS: PLAYER "+event.getPlayer().getName()+" CONNECTED!");
		initPlayer(event.getPlayer());
	}

	/** Called by Rising World when the player spawns into a world after having connected.

		@param	event	the spawn event
	*/
	@EventMethod
	public void onPlayerSpawn(PlayerSpawnEvent event)
	{
//		System.out.println("GPS: PLAYER "+event.getPlayer().getName()+" SPAWNED!");
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
			mainGui(player);
			return;
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
	 * @param event	the change-position event
	*/
	@EventMethod
	public void onPlayerChangePosition(PlayerChangePositionEvent event)
	{
		setGpsText(event.getPlayer());
	}

	//------------------
	// PluginCentral PUBLIC METHODS
	//------------------

	/**
	 * Returns the human-readable, public name of this plug-in (conforms to the PluginCentral interface)
	 * @return	th name of this plug-in
	 */
	public String getPublicName()
	{
		return publicName;
	}

	/**
		Displays the GPS control panel for the given player (conforms to the PluginCentral interface).

		@param	player	the player
	*/
	public void mainGui(Player player)
	{
		int	currWp	= 0;
		if (player.hasAttribute(key_gpsGUIcurrWp))
			currWp	= (int)player.getAttribute(key_gpsGUIcurrWp);
		GpsGUI	gui		= new GpsGUI(this, player, gpsYPos, currWp);
		gui.show(player);
	}

	/**
	 * Returns the version for this plug-in (conforms to the PluginCentral 2 interface).
	 * @return the plug-in version as an int.
	 */
	public int version()
	{
		return VERSION_INT;
	}

	//------------------
	// PUBLIC METHODS
	//
	// (These methods are available to other plug-ins too)
	//------------------

	/**
		Turns on/off the GPS display.

		@param	player	the player for whom to toggle the GSP display
	 	@param	show	true to show | false to hide
	*/
	public void setGPSShow(Player player, boolean show)
	{
		player.setAttribute(key_gpsShow, show);
		GuiLabel labelgpsInfo = (GuiLabel) player.getAttribute(key_gpsLabel);
		if (labelgpsInfo != null)
			labelgpsInfo.setVisible(show);
		setGpsText(player);							// update displayed text
	}

	/**
		Toggles on/off the display of home way-point data

		@param	player	the player for whom to change the home display
	*/
	public void setShowHome(Player player)
	{
		player.setAttribute(key_gpsHomeShow, !(boolean)player.getAttribute(key_gpsHomeShow) );
		setGpsText(player);				// update displayed text
	}

	/**
		Controls the display of way-point data.

		@param	player	the affected player.
	 	@param	index	an integer from 1 to 15 to display the corresponding way-point
		*				or -1 to display next target point
						or 0 to turn way-point display off
	*/
	public void setShowWp(Player player, Integer index)
	{
		// check index is there and is legal
		if (index == null || index < TARGET_ID || index > MAX_WP)
		{
			player.sendTextMessage(Msgs.msg[Msgs.err_showWpInvalidIndex]);
			return;
		}
		// if not turning off (index = 0), check that waypoint exists
		if (index == TARGET_ID && player.getAttribute(key_gpsTargetList) == null ||
			index > 0 && ((Waypoint[])player.getAttribute(key_gpsWpList))[index] == null)
		{
			player.sendTextMessage(String.format(Msgs.msg[Msgs.err_showWpUndefinedWp], index));
			return;
		}
		player.setAttribute(key_gpsWpShow, index);
		setGpsText(player);						// update displayed text
	}

	/**
	 * Adds the given point to the list of targets for player 'player'
	 * @param	player	the player to whom add the target
	 * @param	x		the target x coordinate
	 * @param	y		the target y coordinate
	 * @param	z		the target z coordinate
	 * @param	name	a human readable name for the target
	 */
	public void addTarget(Player player, String name, float x, float y, float z)
	{
		List<Waypoint> targets	= (List<Waypoint>)player.getAttribute(key_gpsTargetList);
		if (targets == null)
		{
			targets	= new ArrayList<>();
			player.setAttribute(key_gpsTargetList, targets);
		}
		Waypoint	wp	= new Waypoint(TARGET_ID, name, x, y, z);
		targets.add(wp);
		setShowWp(player, TARGET_ID);
	}

	/**
		Tele-transports to the index-th way-point.
		The way-point must be defined and, if different from Home, teleport to
		way-points must be enabled.

	 	@param	player	the affected player.
	 	@param	index	a int from 0 to 9 with the index of the way-point.
	*/
	public void teleportToWp(Player player, Integer index)
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
	void setGpsText(Player player)
	{
		if (player == null)
			return;
		if (player.getAttribute(key_gpsLabel) == null)
			return;
		GuiLabel labelgpsInfo = (GuiLabel) player.getAttribute(key_gpsLabel);
		if (labelgpsInfo == null)
			return;

		// check for targets
		Vector3f		playerPos	= player.getPosition();
		List<Waypoint>	targets		= (List<Waypoint>)player.getAttribute(key_gpsTargetList);
		if (targets != null && !targets.isEmpty())
		{
			Waypoint	wp			= targets.get(0);
			float		dist		= (playerPos.x - wp.pos.x)*(playerPos.x - wp.pos.x)+
										(playerPos.z - wp.pos.z)*(playerPos.z - wp.pos.z);
			// has the first target in the list been reached?
			if (dist < TARGET_MIN_DIST)
			{
				// yes, remove it from the list
				targets.remove(0);
				// if list is empty, remove it as an attribute and turn wp display off
				if (targets.isEmpty())
				{
					player.setAttribute(key_gpsTargetList, null);
					player.setAttribute(key_gpsWpShow, 0);
				}
			}
		}

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
			int			posW		= (int)playerPos.x;
			int			posE		= -posW;
			int			posN		= (int)playerPos.z;
			int			posH		= (int)playerPos.y;
			String		latDir		= "";
			String		longDir		= "";
			if (!coordNativeFormat)
			{
				// set N/S and E/W according to signs of coordinates
				latDir		= Msgs.msg[Msgs.txt_north];
				if (posN < 0)
				{
					posN	= -posN;
					latDir	= Msgs.msg[Msgs.txt_south];
				}
				longDir	= Msgs.msg[Msgs.txt_east];
				if (posE < 0)
				{
					posE	= -posE;
					longDir	= Msgs.msg[Msgs.txt_west];
				}
			}
			// OUTPUT: home
			String		text		= "";
			Waypoint[]	wps			= (Waypoint[])player.getAttribute(key_gpsWpList);
			Waypoint	home;
			if ((boolean)player.getAttribute(key_gpsHomeShow) && wps != null && (home=wps[HOME_WP]) != null)
				text = home.toString(heading, playerPos) + " | ";
			// main data
			text	+= String.format("%03d°", hdg) + (!coordNativeFormat ?
					(" (" + posN + latDir + "," + posE + longDir +") h" + posH) :
					(" (" + posW + "," + posH + "," + posN + ")"));
			// waypoint
			int			wpToShow	= (int) player.getAttribute(key_gpsWpShow);
			Waypoint	wp			= null;
			if (wpToShow == TARGET_ID)
			{
				if (targets != null && !targets.isEmpty())
					wp	= targets.get(0);
			}
			else if (wpToShow > 0)
			{
				wp		= ((Waypoint[])player.getAttribute(key_gpsWpList))[wpToShow];
			}
			if (wp != null)
				text += " | " + wp.toString(heading, playerPos);

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

	/**
		Initialises settings from settings file.
	*/
	private void initSettings()
	{
		// create and load settings from disk
		Properties settings	= new Properties();
		// NOTE : use getResourcesAsStream() if the setting file is included in the distrib. .jar)
		FileInputStream in;
		try {
			in = new FileInputStream(getPath() + "/settings.properties");
			settings.load(new InputStreamReader(in, "UTF8"));
			in.close();
			// fill global values
			commandPrefix	= "/" + settings.getProperty("command", commandPrefix);
			allowTpToWp		= (toInteger(settings.getProperty("allowTpToWp"), 0) != 0);
			coordNativeFormat= (toInteger(settings.getProperty("coordNativeFormat"), 0) != 0);
			gpsYPos			= Float.parseFloat(settings.getProperty("gpsYPos", Float.toString(gpsYPos)));
			wpDispLen		= toInteger(settings.getProperty("wpDispLength"), wpNameDispLenDef);
			wpHdgPrecis		= toInteger(settings.getProperty("wpHdgPrecis"), wpHdgPrecisDef);
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
//			return;					// settings are init'ed anyway: on exception, do nothing
		}
	}

	/**
		Returns txt as an integer number if it can be interpreted as one or defValue if it cannot.

 		@param	txt			the String to interpret as an int.
 		@param	defValue	the value to return on error.
 		@return	the equivalent int or defValue if txt cannot represent an integer.
	 */
	static int toInteger(String txt, int defValue)
	{
		if (txt == null)
			return defValue;
		int val;
		try {
			val = Integer.decode(txt);
		} catch (NumberFormatException e) {		// txt cannot be parsed as a number
			return defValue;
		}
		return val;
	}

}

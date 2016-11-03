/****************************
	G P S  -  A Java plug-in for Rising World.

	Msgs.java - a data-only class with interface texts, separated for easier translation.
	 			Eventually replaced by using Messages.getString().

	Created by : Maurizio M. Gavioli 2016-08-15

	(C) Maurizio M. Gavioli (a.k.a. Miwarre), 2016
	Licensed under the Creative Commons by-sa 3.0 license (see http://creativecommons.org/licenses/by-sa/3.0/ for details)

*****************************/

package com.vistamaresoft.gps;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
//import java.util.MissingResourceException;
import java.util.Properties;
//import java.util.ResourceBundle;

public class Msgs
{
	// TEXT IDENTIFIERS
	public static final int	err_invalidCmd			= 0;
	public static final int	err_noTpToWp			= 1;
	public static final int	err_setWpDuplIdx		= 2;
	public static final int	err_setWpDuplName		= 3;
	public static final int	err_setWpInvalidIndex	= 4;
	public static final int	err_showWpInvalidIndex	= 5;
	public static final int	err_showWpUndefinedWp	= 6;

	// Info messages
	public static final int	msg_deinit		= 7;
	public static final int	msg_init		= 8;
	public static final int	msg_homeSet		= 9;
	public static final int	msg_wpList		= 10;
	public static final int	msg_wpSet		= 11;

	// GUI texts
	public static final int	button_close	= 12;
	public static final int	button_goto		= 13;
	public static final int	button_off		= 14;
	public static final int	button_on		= 15;
	public static final int	button_homehide	= 16;
	public static final int	button_homeset	= 17;
	public static final int	button_homeshow	= 18;
	public static final int	button_next		= 19;
	public static final int	button_prev		= 20;
	public static final int	button_wphide	= 21;
	public static final int	button_wpset	= 22;
	public static final int	button_wpshow	= 23;

	// Various texts
	public static final int	txt_homeName	= 24;
	public static final int txt_help_from	= 25;
	public static final int	txt_help_to		= 37;
	public static final int	txt_undefined	= 38;

	private static final int	LAST_TEXT	= txt_undefined;

	public static			String[]	msg			=
	{
		"Invalid GPS command: '",
		"GPS goto: teleport to waypoints is disabled.",
		"GPS setwp: duplicate index",
		"GPS setwp: duplicate name",
		"GPS setwp: waypoint index must be an integer between "+Gps.minWpProper+" and "+Gps.maxWp,
		"GPS goto/wp: waypoint index must be an integer between "+Gps.minWp+" and "+Gps.maxWp,
		"GPS goto/wp: waypoint %d is undefined",
		"GPS "+Gps.version+" unloaded successfully!",
		"GPS "+Gps.version+" loaded successfully!",
		"GPS: Home point set.",
		"GPS defined waypoints:",
		"GPS: Waypoint %1$d '%2$s' set.",
		"CLOSE",
		"GO\nTO",
		"TURN OFF",
		"TURN ON",
		"HIDE\nHOME",
		"SET\nHOME",
		"SHOW\nHOME",
		" > ", 
		" < ", 
		"HIDE\nWP",
		"SET\nWP",
		"SHOW\nWP",
		"Home",
		"[#00ff40]GPS Help",
		"[#00ff40]/gps [#ffffff]opens the GPS GUI",
		"[#00ff40]/gps on [#ffffff]turns whole GPS display on",
		"[#00ff40]/gps off [#ffffff]turns whole GPS display off",
		"[#00ff40]/gps sethome [#ffffff]set home position to current position",
		"[#00ff40]/gps showhome [#ffffff]toggles home data display on/off",
		"[#00ff40]/gps setwp [n] [name] [#ffffff]sets n-th wpt with name 'name'",
		"[#00ff40]/gps showwp <n> [#ffffff]turns wpt display on (n=0 to turn off)",
		"[#00ff40]/gps list [#ffffff]lists defined waypoints (incl. home)",
		"[#00ff40]/gps home [#ffffff]teleports to home (if defined)",
		"[#00ff40]/gps goto <n> [#ffffff]teleports to n-th wp (if defined)",
		"[#00ff40]/home [#ffffff]same of '/gps home' (for compatibility)",
		"[#00ff40]/sethome [#ffffff]same as '/gps sethome' (for compatiblity)",
		"--[Undefined]--"
	};

	private static final	String		BUNDLE_NAME	= "/locale/messages";

	public static boolean init(String path, Locale locale)
	{
		if (locale == null)
			return false;
		String		country		= locale.getCountry();
		String		variant		= locale.getVariant();
		String		fname		= BUNDLE_NAME + "_" + locale.getLanguage();
		if (country.length() > 0)	fname += "_" + country;
		if (variant.length() > 0)	fname += "_" + variant;
		fname	+= ".properties";
		Properties settings	= new Properties();
		// NOTE : use getResourcesAsStream() if the setting file is included in the distrib. .jar)
		FileInputStream in;
		try
		{
		in = new FileInputStream(path + fname);
		settings.load(in);
		in.close();
		} catch (IOException e) {
			System.out.println("** GPS plug-in ERROR: Property file '" + fname + "' for requested locale '"+ locale.toString() + "' not found. Defaulting to 'en'");
			return false;
		}
		// Load strings from localised bundle
		for (int i = 0; i <= LAST_TEXT; i++)
			msg[i]	= settings.getProperty(String.format("%03d", i) );
		// a few strings require additional steps
		msg[err_setWpInvalidIndex]	= String.format(msg[err_setWpInvalidIndex], Gps.minWpProper, Gps.maxWp);
		msg[err_showWpInvalidIndex]	= String.format(msg[err_showWpInvalidIndex], Gps.minWp, Gps.maxWp);
		msg[msg_deinit]				= String.format(msg[msg_deinit], Gps.version);
		msg[msg_init]				= String.format(msg[msg_init], Gps.version);
		return true;
	}
}

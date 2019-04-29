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
import java.io.InputStreamReader;
import java.util.Locale;
//import java.util.MissingResourceException;
import java.util.Properties;
//import java.util.ResourceBundle;

class Msgs
{
	// TEXT IDENTIFIERS
	static final int	err_noTpToWp			= 0;
	static final int	err_showWpInvalidIndex	= 1;
	static final int	err_showWpUndefinedWp	= 2;

	// Info messages
	static final int	msg_homeSet		= 3;
	static final int	msg_wpSet		= 4;
	static final int	msg_homeDel		= 5;
	static final int	msg_wpDel		= 6;

	// Various texts
	static final int	txt_homeName	=  7;
	static final int	txt_undefined	=  8;
	static final int	txt_gpsHelpHint	=  9;
	static final int	txt_wpNameTitle	= 10;
	static final int	txt_wpNameCapt	= 11;
	static final int	txt_north		= 12;
	static final int	txt_east		= 13;
	static final int	txt_south		= 14;
	static final int	txt_west		= 15;
//	static final int	txt_wp_selector	= 16;
//	static final	int	txt_no_wp		= 16;
//	static final int	txt_share		= 18;
//	static final int	txt_wp_shared	= 19;
//	static final int	txt_wp_existed	= 20;
//	static final int	txt_error		= 17;

	private static final int	LAST_TEXT	= txt_west;

	static			String[]	msg			=
	{
		"GPS goto: teleport to waypoints is disabled.",
		"GPS goto/wp: waypoint index must be an integer between "+Gps.MIN_WP+" and "+Gps.MAX_WP,
		"GPS goto/wp: waypoint %d is undefined",
		"GPS: Home point set.",
		"GPS: Waypoint %1$d '%2$s' set.",
		"GPS: Home point deleted.",
		"GPS: Waypoint %d deleted.",
		"Home",
		"--[Undefined]--",
		"Chat '/$s' for control panel",
		"Waypoint name",
		"Enter the name for the new waypoint and press ENTER",
		"N",
		"E",
		"S",
		"W",
//		"Waypoints to share",
//		"[No waypoint defined]",
//		"\n SHARE \n ",
//		"GPS: Waypoint %d has been shared.",
//		"GPS: Waypoint %d was already shared.",
//		"GPS: Internal error (%d)."
	};

	private static final	String		BUNDLE_NAME	= "/locale/messages";

	static boolean init(String path, Locale locale)
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
		settings.load(new InputStreamReader(in, "UTF8"));
		in.close();
		} catch (IOException e) {
			System.out.println("** GPS plug-in ERROR: Property file '" + fname + "' for requested locale '"+ locale.toString() + "' not found. Defaulting to 'en'");
			return false;
		}
		// Load strings from localised bundle
		for (int i = 0; i <= LAST_TEXT; i++)
			msg[i]	= settings.getProperty(String.format("%03d", i) );
		// a few strings require additional steps
		msg[err_showWpInvalidIndex]	= String.format(msg[err_showWpInvalidIndex], Gps.MIN_WP, Gps.MAX_WP);
		msg[txt_gpsHelpHint]		= String.format(msg[txt_gpsHelpHint], Gps.commandPrefix);
//		msg[txt_share]				= "\n " + msg[txt_share] + " \n ";
		return true;
	}
}

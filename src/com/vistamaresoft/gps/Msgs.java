/****************************
	G P S P l u g i n  -  A Java plug-in for Rising World.

	Msgs.java - a data-only class with interface texts, separated for easier translation.
	 			Might be replaced with gettext().

	Created by : Maurizio M. Gavioli 2016-08-15

	(C) Maurizio M. Gavioli (a.k.a. Miwarre), 2016
	Licensed under the Creative Commons by-sa 3.0 license (see http://creativecommons.org/licenses/by-sa/3.0/ for details)

*****************************/

package com.vistamaresoft.gps;

public class Msgs
{
	// Error messages
	static final public String	err_gspInvalidCmd		= "Invalid GPS command: '";
	static final public String	err_noTpToWp			= "GPS got: teleport to waypoints is disabled";
	static final public String	err_setWpDuplIdx		= "GPS setwp: duplicate index";
	static final public String	err_setWpDuplName		= "GPS setwp: duplicate name";
	static final public String	err_setWpInvalidIndex	= "GPS setwp: waypoint index must be an integer between "+GpsPlugin.minWpProper+" and "+GpsPlugin.maxWp;
	static final public String	err_showWpInvalidIndex	= "GPS goto/wp: waypoint index must be an integer between "+GpsPlugin.minWp+" and "+GpsPlugin.maxWp;
	static final public String	err_showWpUndefinedWp	= "GPS goto/wp: waypoint %d is undefined";

	// Info messages
	static final public String	msg_init		= "GPS loaded successfully! (version "+GpsPlugin.gpsVersion+")";
	static final public String	msg_homeSet		= "GPS: Home point set.";
	static final public String	msg_wpList		= "GPS defined waypoints:";
	static final public String	msg_wpSet		= "GPS: Waypoint %d '%s' set.";

	// Various texts
	static final public String	txt_homeName	= "Home";
	static final public String[]txt_help		= { "[#00ff40]GPS Help",
			"[#00ff40]/gps [#ffffff]toggles whole GPS display on/off",
			"[#00ff40]/gps on [#ffffff]turns whole GPS display on",
			"[#00ff40]/gps off [#ffffff]turns whole GPS display off",
			"[#00ff40]/gps sethome [#ffffff]set home position to current position",
			"[#00ff40]/gps showhome [#ffffff]toggles home data display on/off",
			"[#00ff40]/gps setwp [n] [name] [#ffffff]sets n-th wpt with name 'name'",
			"[#00ff40]/gps showwp <n> [#ffffff]turns wpt display on (n=0 to turn off)",
			"[#00ff40]/gps list [#ffffff]lists defined waypoints (incl. home)",
			"[#00ff40]/gps home [#ffffff]teleports to home (if defined)",
			"[#00ff40]/home [#ffffff]same of '/gps home' (for compatibility)",
			"[#00ff40]/sethome [#ffffff]same as '/gps sethome' (for compatiblity)",
			};

}

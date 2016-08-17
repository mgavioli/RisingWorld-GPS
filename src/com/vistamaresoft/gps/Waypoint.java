/****************************
	G P S P l u g i n  -  A Java plug-in for Rising World.

	Waypoint.java - A waypoint

	Created by : Maurizio M. Gavioli 2016-08-15

	(C) Maurizio M. Gavioli (a.k.a. Miwarre), 2016
	Licensed under the Creative Commons by-sa 3.0 license (see http://creativecommons.org/licenses/by-sa/3.0/ for details)

*****************************/

package com.vistamaresoft.gps;

import net.risingworld.api.utils.Vector3f;

public class Waypoint
{

	protected	int			id;
	protected	String		name;
	protected	Vector3f	pos;

	// C'TOR

	Waypoint(int id, String name, float x, float y, float z)
	{
		this.id		= id;
		this.name	= name;
		this.pos.setX(x);
		this.pos.setY(y);
		this.pos.setZ(z);
	}

	// toString()
	//
	// Returns a textual representation of the waypoint suitable for a list
	public String toString()
	{
		if (name == null || name.length() < 1)		// undefined
			return null;
//		return ""+id+": "+name+" ("+Math.floor(pos.z)+"N,"+(-Math.floor(pos.x))+"E) h"+Math.floor(pos.y);	
		return String.format("%d: %s (%dN,%dE) h%d",
				id, name, (int)Math.floor(pos.z), -(int)Math.floor(pos.x), (int)Math.floor(pos.y) );	
	}

	// toString(double, Vector3f)
	//
	// Returns a textual representation of the waypoint suitable for the GPS GUI output
	public String toString(double playerHdg, Vector3f playerPos)
	{
		double	deltaN		= pos.z - playerPos.z;
		double	deltaW		= pos.x - playerPos.x;
		double	dist		= Math.sqrt(deltaN * deltaN + deltaW * deltaW);	// distance in blocks
		double	radial;
		if (dist < 4)			// if distance less than 2 m, data are unreliable, only output wp name
			return  " | ---°  " + name.substring(0, GpsPlugin.nameDispLen) + "   <2m";
		else
		{
			radial			= Math.acos(deltaN / dist) * GpsPlugin.rad2deg;
			if (deltaW > 0)
				radial 		= 360 - radial;		// for this adjustment,  see setGPSText() above
			radial			= Math.floor(radial + 0.5);
			if (radial == 0)
				radial = 360;
		}
		// text build up
		double	wpHdgDelta	= playerHdg - radial;
		String	text		= String.format("%03d°", radial);				// separator and radial
		if ( (wpHdgDelta > GpsPlugin.wpHdgPrecis && wpHdgDelta < (180-GpsPlugin.wpHdgPrecis))	// left arrow
				|| (wpHdgDelta > (GpsPlugin.wpHdgPrecis-360) && wpHdgDelta < (-GpsPlugin.wpHdgPrecis-180)) )
			text += " <";
		else
			text += "  ";

		text  += name.substring(0, GpsPlugin.nameDispLen);					// wp name

		if ( (wpHdgDelta < -GpsPlugin.wpHdgPrecis && wpHdgDelta > (GpsPlugin.wpHdgPrecis-180))	// right arrow
				|| (wpHdgDelta < (360-GpsPlugin.wpHdgPrecis) && wpHdgDelta > (GpsPlugin.wpHdgPrecis+180)) )
			text += "> ";
		else
			text += "  ";

		text += Math.floor(dist / 2 + 0.5) + "m";							// distance in m
		return text;
	}
}

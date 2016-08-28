/****************************
	G P S P l u g i n  -  A Java plug-in for Rising World.

	Db.java - The database management class

	Created by : Maurizio M. Gavioli 2016-08-15

	(C) Maurizio M. Gavioli (a.k.a. Miwarre), 2016
	Licensed under the Creative Commons by-sa 3.0 license (see http://creativecommons.org/licenses/by-sa/3.0/ for details)

*****************************/

package com.vistamaresoft.gps;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.risingworld.api.Plugin;
import net.risingworld.api.database.WorldDatabase;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

public class Db
{
	// Globals
	// TODO : replace with plug-in DB!
	static	protected	WorldDatabase	db			= Plugin.getWorldDatabase();

	//
	//	init()
	//
	//	Initialises the DB. Can be run at each script startup without destroying existing data.
	static void dbInit()
	{
		db.execute(
			"CREATE TABLE IF NOT EXISTS `waypoints` ("
			+ "`player_name` CHAR(64) NOT NULL DEFAULT ('[NoName]'),"
			+ "`wp_name`     CHAR(64) NOT NULL DEFAULT ('[NoName]'),"
			+ "`wp_id`       INTEGER  NOT NULL,"
			+ "`wp_x`        INTEGER  NOT NULL DEFAULT ( 0 ),"
			+ "`wp_y`        INTEGER  NOT NULL DEFAULT ( 0 ),"
			+ "`wp_z`        INTEGER  NOT NULL DEFAULT ( 0 ),"
			+ "PRIMARY KEY (player_name, wp_id) "
			+ ");");
	}

	//
	//	loadPlayer(player)
	//
	// Loads from the DB the Home/wp data for a player and caches them in player attributes

	static void dbLoadPlayer(Player player)
	{
		ResultSet	result;
		Waypoint	waypoints[]	= new Waypoint[GpsPlugin.maxWp+1];
		try {
			result = db.executeQuery("SELECT * FROM `waypoints` WHERE `player_name` = '" + player.getName() + "' ORDER BY `wp_id`;");
			try {
				while (result.next())
				{
					waypoints[result.getInt("wp_id")] =
							new Waypoint(
									result.getInt("wp_id"),
									result.getString("wp_name"),
									result.getFloat("wp_x"),
									result.getFloat("wp_y"),
									result.getFloat("wp_z")
									);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		player.setAttribute(GpsPlugin.key_gpsWpList, waypoints);
	}

	// setWp(player, wpIndex, wpName)
	//
	// Inserts into the DB (or replace if already present) data for a waypoint,
	// also updating the player attribute cache

	static void setWp(Player player, int wpIndex, String wpName)
	{
		String		playerName	= player.getName();
		Vector3f	playerPos	= player.getPosition();
		// update DB
		db.executeUpdate(
				"INSERT OR REPLACE INTO waypoints (player_name,wp_name,wp_id,wp_x,wp_y,wp_z) VALUES ('"
				+playerName+"','"+wpName+"',"+wpIndex+","+playerPos.x+","+playerPos.y+","+playerPos.z+");"
				);
		// update player cache
		Waypoint	wp			= new Waypoint(wpIndex, wpName, playerPos.x, playerPos.y, playerPos.z);
		((Waypoint[])player.getAttribute(GpsPlugin.key_gpsWpList))[wpIndex]	= wp;

		if (wpIndex == 0)
			player.sendTextMessage(Msgs.msg_homeSet);
		else
			player.sendTextMessage(String.format(Msgs.msg_wpSet, wpIndex, wpName));
	}

	static public void setHome(Player player)
	{
		setWp(player, 0, Msgs.txt_homeName);
	}
}

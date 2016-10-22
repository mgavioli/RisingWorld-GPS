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
import net.risingworld.api.database.Database;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

public class Db
{
	// Globals
	static	protected	Database	db	= null;

	/**
		Initialises and opens the DB for this plug-in. Can be run at each
		script startup without destroying existing data.

	 	@param	plugin	the plug-in instance for which to initialise the DB.
	 */
	static void init(Plugin plugin)
	{
		if (db == null)
			db = plugin.getSQLiteConnection(plugin.getPath() + "/gps-"+plugin.getWorld().getName()+".db");

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
	static void deinit()
	{
		db.close();
		db = null;
	}

	/**
		Loads from the DB the Home/way-point data for a player and caches them
		in player attributes.

	 	@param	player	the target player.
	*/
	static void loadPlayer(Player player)
	{
		ResultSet	result;
		Waypoint	waypoints[]	= new Waypoint[Gps.maxWp+1];
		player.setAttribute(Gps.key_gpsWpList, waypoints);
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
				e.printStackTrace();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
		Inserts into the DB (or replace if already present) data for a
		way-point at current player position, also updating the player
		attribute cache.

		@param	player	the affected player
		@param	wpIndex	the index (0 - 9) of the new waypoint
		@param	wpName	the name of the new way-point.
	*/
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
		Waypoint	wp			= new Waypoint(wpIndex, wpName, playerPos.getX(), playerPos.getY(), playerPos.getZ());
		((Waypoint[])player.getAttribute(Gps.key_gpsWpList))[wpIndex]	= wp;

		if (wpIndex == 0)
			player.sendTextMessage(Msgs.msg_homeSet);
		else
			player.sendTextMessage(String.format(Msgs.msg_wpSet, wpIndex, wpName));
	}

	/**
		Inserts into the DB (or replace if already present) Home data
		at current player position, also updating the player attribute cache.

		@param	player	the affected player
	 */
	static public void setHome(Player player)
	{
		setWp(player, 0, Msgs.txt_homeName);
	}
}

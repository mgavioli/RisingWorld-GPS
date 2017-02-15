/****************************
	G P S  -  A Java plug-in for Rising World.

	Db.java - The database management class

	Created by : Maurizio M. Gavioli 2016-08-15

	(C) Maurizio M. Gavioli (a.k.a. Miwarre), 2016
	Licensed under the Creative Commons by-sa 3.0 license (see http://creativecommons.org/licenses/by-sa/3.0/ for details)

*****************************/

package com.vistamaresoft.gps;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import net.risingworld.api.Plugin;
import net.risingworld.api.database.Database;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

public class Db
{
	// Globals
	static	protected	Database	db	= null;

	public static final	int			ERROR_OK			= 0;
	public static final	int			ERROR_DB			= -1;
	public static final	int			ERROR_INVALIDARG	= -2;

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
		Waypoint	waypoints[]	= new Waypoint[Gps.MAX_WP+1];
		player.setAttribute(Gps.key_gpsWpList, waypoints);
		try (ResultSet result = db.executeQuery("SELECT * FROM `waypoints` WHERE `player_name` = '"
				+ player.getName() + "' ORDER BY `wp_id`;"))
		{
				while (result.next())
				{
					int	wpIdx	= result.getInt("wp_id");
					if (wpIdx < Gps.MIN_WP || wpIdx >= Gps.MAX_WP)
						continue;
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
	}

	static Waypoint[] getGlobalWps()
	{
		ArrayList<Waypoint>	waypoints	= new ArrayList<Waypoint>();
		try (ResultSet result = db.executeQuery(
				"SELECT * FROM `waypoints` WHERE `player_name` = '    ';"))
		{
			while (result.next())
			{
				waypoints.add(new Waypoint(
								result.getInt("wp_id"),
								result.getString("wp_name"),
								result.getFloat("wp_x"),
								result.getFloat("wp_y"),
								result.getFloat("wp_z")
								)
				);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return waypoints.toArray(new Waypoint[waypoints.size()]);
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
		setWp(player, wpIndex, player.getPosition(), wpName);
	}

	static void setWp(Player player, int wpIdx, Vector3f pos, String wpName)
	{
		if (wpIdx < Gps.MIN_WP || wpIdx >= Gps.MAX_WP)
			return;
		String		playerName	= player.getName();
		// update DB
		db.executeUpdate(
				"INSERT OR REPLACE INTO waypoints (player_name,wp_name,wp_id,wp_x,wp_y,wp_z) VALUES ('"
				+playerName+"','"+wpName+"',"+wpIdx+","+pos.x+","+pos.y+","+pos.z+");"
				);
		// update player cache
		Waypoint	wp			= new Waypoint(wpIdx, wpName, pos.x, pos.y, pos.z);
		((Waypoint[])player.getAttribute(Gps.key_gpsWpList))[wpIdx]	= wp;

		if (wpIdx == 0)
			player.sendTextMessage(Msgs.msg[Msgs.msg_homeSet]);
		else
			player.sendTextMessage(String.format(Msgs.msg[Msgs.msg_wpSet], wpIdx, wpName));
	}

	static int deleteWp(Player player, int wpIdx)
	{
		if (wpIdx < Gps.MIN_WP || wpIdx >= Gps.MAX_WP)
			return ERROR_INVALIDARG;
		String		playerName	= player.getName();
		// update DB
		db.executeUpdate(
				"DELETE FROM waypoints WHERE player_name = '"+playerName+"' AND wp_id="+wpIdx+";"
				);
		// update player cache
		((Waypoint[])player.getAttribute(Gps.key_gpsWpList))[wpIdx]	= null;

		if (wpIdx == 0)
			player.sendTextMessage(Msgs.msg[Msgs.msg_homeDel]);
		else
			player.sendTextMessage(String.format(Msgs.msg[Msgs.msg_wpDel], wpIdx));
		return ERROR_OK;
	}

	static int shareWp(Player player, int wpIdx)
	{
		if (wpIdx < Gps.MIN_WP || wpIdx >= Gps.MAX_WP)
			return ERROR_INVALIDARG;
		Waypoint	wp			= ((Waypoint[])player.getAttribute(Gps.key_gpsWpList))[wpIdx];
		if (wp == null)
			return ERROR_INVALIDARG;
		String		wpName	= wp.name + " (" + player.getName() + ")";
		// prepare name parameter to avoid quoting issues
		String query	= "INSERT OR REPLACE INTO `waypoints` (player_name,wp_name,wp_id,wp_x,wp_y,wp_z) VALUES ('    ',?,0,"
				+wp.pos.x+","+wp.pos.y+","+wp.pos.z+");";
		try(PreparedStatement stmt	= db.getConnection().prepareStatement(query)
		)
		{
			stmt.setString(1, wpName);
			stmt.executeUpdate();
		} catch (SQLException e)
		{
			e.printStackTrace();
			return ERROR_DB;
		}
		return ERROR_OK;
	}

	/**
		Inserts into the DB (or replace if already present) Home data
		at current player position, also updating the player attribute cache.

		@param	player	the affected player
	 */
	static public void setHome(Player player)
	{
		setWp(player, 0, Msgs.msg[Msgs.txt_homeName]);
	}
}

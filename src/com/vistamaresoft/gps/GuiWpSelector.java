/****************************
	G P S  -  A Java plug-in for Rising World.

	GuiWpSelector.java - A GuiModalWindow to list and select all defined WP's by a player.

	Created by : Maurizio M. Gavioli 2017-03-20

	(C) Maurizio M. Gavioli (a.k.a. Miwarre), 2017
	Licensed under the Creative Commons by-sa 3.0 license (see http://creativecommons.org/licenses/by-sa/3.0/ for details)

*****************************/

package com.vistamaresoft.gps;

import com.vistamaresoft.rwgui.GuiDialogueBox;
import com.vistamaresoft.rwgui.GuiHorizontalLayout;
import com.vistamaresoft.rwgui.RWGui;
import com.vistamaresoft.rwgui.RWGui.RWGuiCallback;
import net.risingworld.api.gui.GuiImage;
import net.risingworld.api.gui.GuiLabel;
import net.risingworld.api.objects.Player;

public class GuiWpSelector extends GuiDialogueBox
{
	// Constants
	private static final	int		DOBUTTON_ID	=  Gps.MAX_WP + 1;
	//
	// FIELDS
	//
	private	GuiImage[]		checkBoxes;
	private	int				checkedFlags;
	private	RWGuiCallback	myCallback;

	public GuiWpSelector(Gps plugin, Player player, RWGuiCallback callback)
	{
		super(plugin, Msgs.msg[Msgs.txt_wp_selector], RWGui.LAYOUT_VERT, null);
		setCallback(new DlgHandler());
		myCallback	= callback;
		boolean				added	= false;
		checkBoxes					= new GuiImage[Gps.MAX_WP - Gps.MIN_WP + 1];
		checkedFlags				= 0;
		GuiHorizontalLayout	layout;
		Waypoint[]			waypoints = (Waypoint[]) player.getAttribute(Gps.key_gpsWpList);
		Waypoint			wp;
		if (waypoints != null)
			for (int i=Gps.MIN_WP_PROPER; i <= Gps.MAX_WP; i++)
			{
				if ( (wp=waypoints[i]) != null)
				{
					layout	= (GuiHorizontalLayout)addNewLayoutChild(RWGui.LAYOUT_HORIZ,
							RWGui.LAYOUT_H_LEFT | RWGui.LAYOUT_V_MIDDLE);
					checkBoxes[i]	= new GuiImage(0, 0, false, RWGui.BUTTON_SIZE, RWGui.BUTTON_SIZE, false);
					RWGui.setImage(checkBoxes[i], RWGui.ICN_UNCHECK);
					layout.addChild(checkBoxes[i], i);
					layout.addChild(new GuiLabel(""+i+". "+wp.name, 0, 0, false), i);
					added	= true;
				}
			}
		if (added)
		{
			layout	= (GuiHorizontalLayout)addNewLayoutChild(RWGui.LAYOUT_HORIZ,
					RWGui.LAYOUT_H_CENTRE | RWGui.LAYOUT_V_MIDDLE);
			GuiLabel	label	= new GuiLabel(Msgs.msg[Msgs.txt_share], 0, 0, false);
			layout.addChild(label, DOBUTTON_ID);
			label.setColor(RWGui.ACTIVE_COLOUR);
		}
		else
		{
			addChild(new GuiLabel(Msgs.msg[Msgs.txt_no_wp], 0, 0, false));
		}
	}

	//********************
	// HANDLERS
	//********************

	private class DlgHandler implements RWGuiCallback
	{
		@Override
		public void onCall(Player player, int id, Object data)
		{
			if (id >= Gps.MIN_WP_PROPER && id <= Gps.MAX_WP)
			{
				int		flagFlip	= 1 << id;
				checkedFlags		^= flagFlip;
				RWGui.setImage(checkBoxes[id], (checkedFlags & flagFlip) != 0 ? RWGui.ICN_CHECK : RWGui.ICN_UNCHECK);	return;
			}
			switch (id)
			{
			case RWGui.ABORT_ID:
				myCallback.onCall(player, RWGui.ABORT_ID, checkedFlags);
				break;
			case DOBUTTON_ID:
				myCallback.onCall(player, RWGui.OK_ID, checkedFlags);
				pop(player);
				break;
			}
		}
	}

}
